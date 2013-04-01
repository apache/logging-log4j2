/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.async;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusLogger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.Util;

/**
 * AsyncLogger is a logger designed for high throughput and low latency logging.
 * It does not perform any I/O in the calling (application) thread, but instead
 * hands off the work to another thread as soon as possible. The actual logging
 * is performed in the background thread. It uses the LMAX Disruptor library for
 * inter-thread communication. (<a
 * href="http://lmax-exchange.github.com/disruptor/"
 * >http://lmax-exchange.github.com/disruptor/</a>)
 * <p>
 * To use AsyncLogger, specify the System property
 * -DLog4jContextSelector=log4j.async.AsyncLoggerContextSelector before you
 * obtain a Logger, and all Loggers returned by LogManager.getLogger will be
 * AsyncLoggers.
 * <p>
 * Note that for performance reasons, this logger does not support source
 * location. Any %class, %location or %line conversion patterns in your
 * log4j.xml configuration will produce either a "?" character or no output at
 * all.
 * <p>
 * For best performance, use AsyncLogger with the FastFileAppender or
 * FastRollingFileAppender, with immediateFlush=false. These appenders have
 * built-in support for the batching mechanism used by the Disruptor library,
 * and they will flush to disk at the end of each batch. This means that even
 * with immediateFlush=false, there will never be any items left in the buffer;
 * all log events will all be written to disk in a very efficient manner.
 */
public class AsyncLogger extends Logger { // depends on LOG4J2-151
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static volatile Disruptor<RingBufferLogEvent> disruptor;
    private static Clock clock = ClockFactory.getClock();

    private static ExecutorService executor = Executors
            .newSingleThreadExecutor();
    private ThreadLocal<Info> threadlocalInfo = new ThreadLocal<Info>();

    static {
        int ringBufferSize = calculateRingBufferSize();

        WaitStrategy waitStrategy = createWaitStrategy();
        disruptor = new Disruptor<RingBufferLogEvent>(
                RingBufferLogEvent.FACTORY, ringBufferSize, executor,
                ProducerType.MULTI, waitStrategy);
        EventHandler<RingBufferLogEvent>[] handlers = new RingBufferLogEventHandler[] { new RingBufferLogEventHandler() };
        disruptor.handleExceptionsWith(getExceptionHandler());
        disruptor.handleEventsWith(handlers);

        LOGGER.debug(
                "Starting AsyncLogger disruptor with ringbuffer size {}...",
                disruptor.getRingBuffer().getBufferSize());
        disruptor.start();
    }

    private static int calculateRingBufferSize() {
        String userPreferredRBSize = System.getProperty(
                "AsyncLogger.RingBufferSize", "256000");
        int ringBufferSize = 256000; // default
        try {
            int size = Integer.parseInt(userPreferredRBSize);
            if (size < 128) {
                size = 128;
                LOGGER.warn(
                        "Invalid RingBufferSize {}, using minimum size 128.",
                        userPreferredRBSize);
            }
            ringBufferSize = size;
        } catch (Exception ex) {
            LOGGER.warn("Invalid RingBufferSize {}, using default size.",
                    userPreferredRBSize);
        }
        return Util.ceilingNextPowerOfTwo(ringBufferSize);
    }

    private static WaitStrategy createWaitStrategy() {
        String strategy = System.getProperty("AsyncLogger.WaitStrategy");
        LOGGER.debug("property AsyncLogger.WaitStrategy={}", strategy);
        if ("Sleep".equals(strategy)) {
            LOGGER.debug("disruptor event handler uses SleepingWaitStrategy");
            return new SleepingWaitStrategy();
        } else if ("Yield".equals(strategy)) {
            LOGGER.debug("disruptor event handler uses YieldingWaitStrategy");
            return new YieldingWaitStrategy();
        } else if ("Block".equals(strategy)) {
            LOGGER.debug("disruptor event handler uses BlockingWaitStrategy");
            return new BlockingWaitStrategy();
        }
        LOGGER.debug("disruptor event handler uses SleepingWaitStrategy");
        return new SleepingWaitStrategy();
    }

    private static ExceptionHandler getExceptionHandler() {
        String cls = System.getProperty("AsyncLogger.ExceptionHandler");
        if (cls == null) {
            LOGGER.debug("No AsyncLogger.ExceptionHandler specified");
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends ExceptionHandler> klass = (Class<? extends ExceptionHandler>) Class
                    .forName(cls);
            ExceptionHandler result = klass.newInstance();
            LOGGER.debug("AsyncLogger.ExceptionHandler=" + result);
            return result;
        } catch (Exception ignored) {
            LOGGER.debug(
                    "AsyncLogger.ExceptionHandler not set: error creating "
                            + cls + ": ", ignored);
            return null;
        }
    }

    private static class Info {
        RingBufferLogEventTranslator translator;
        String cachedThreadName;
    }

    public AsyncLogger(LoggerContext context, String name,
            MessageFactory messageFactory) {
        super(context, name, messageFactory);
    }

    @Override
    public void log(Marker marker, String fqcn, Level level, Message data,
            Throwable t) {
        Info info = threadlocalInfo.get();
        if (info == null) {
            info = new Info();
            info.translator = new RingBufferLogEventTranslator();
            info.cachedThreadName = Thread.currentThread().getName();
            threadlocalInfo.set(info);
        }

        Boolean includeLocation = config.loggerConfig.isIncludeLocation();
        info.translator.setValues(this, getName(), marker, fqcn, level, data,
                t, //

                // config properties are taken care of in the EventHandler
                // thread in the #actualAsyncLog method

                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableContext(),//

                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableStack(), //

                // Thread.currentThread().getName(), //
                info.cachedThreadName, //

                // location: very expensive operation. LOG4J2-153:
                // Only include if "includeLocation=true" is specified,
                // exclude if not specified or if "false" was specified.
                includeLocation != null && includeLocation ? location(fqcn)
                        : null,

                // System.currentTimeMillis());
                // CoarseCachedClock: 20% faster than system clock, 16ms gaps
                // CachedClock: 10% faster than system clock, smaller gaps
                clock.currentTimeMillis());

        disruptor.publishEvent(info.translator);
    }

    private StackTraceElement location(String fqcnOfLogger) {
        return Log4jLogEvent.calcLocation(fqcnOfLogger);
    }

    /**
     * This method is called by the EventHandler that processes the
     * RingBufferLogEvent in a separate thread.
     * 
     * @param event the event to log
     */
    public void actualAsyncLog(RingBufferLogEvent event) {
        Map<Property, Boolean> properties = config.loggerConfig.getProperties();
        event.mergePropertiesIntoContextMap(properties,
                config.config.getSubst());
        config.logEvent(event);
    }

    public static void stop() {
        Disruptor<RingBufferLogEvent> temp = disruptor;

        // Must guarantee that publishing to the RingBuffer has stopped
        // before we call disruptor.shutdown()
        disruptor = null; // client code fails with NPE if log after stop = OK
        temp.shutdown();

        // wait up to 10 seconds for the ringbuffer to drain
        RingBuffer<RingBufferLogEvent> ringBuffer = temp.getRingBuffer();
        for (int i = 0; i < 20; i++) {
            if (ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize())) {
                break;
            }
            try {
                Thread.sleep(500); // give ringbuffer some time to drain...
            } catch (InterruptedException e) {
            }
        }
        executor.shutdown(); // finally, kill the processor thread
    }

}
