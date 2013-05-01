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
package org.apache.logging.log4j.core.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.Util;

/**
 * Helper class decoupling the {@code AsyncLoggerConfig} class from the LMAX
 * Disruptor library.
 * <p>
 * {@code AsyncLoggerConfig} is a plugin, and will be loaded even if users do
 * not configure any {@code <asyncLogger>} or {@code <asyncRoot>} elements in
 * the configuration. If {@code AsyncLoggerConfig} has inner classes that extend
 * or implement classes from the Disruptor library, a
 * {@code NoClassDefFoundError} is thrown if the Disruptor jar is not in the
 * classpath when the PluginManager loads the {@code AsyncLoggerConfig} plugin
 * from the pre-defined plugins definition file.
 * <p>
 * This class serves to make the dependency on the Disruptor optional, so that
 * these classes are only loaded when the {@code AsyncLoggerConfig} is actually
 * used.
 */
class AsyncLoggerConfigHelper {

    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 20;
    private static final int HALF_A_SECOND = 500;
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static volatile Disruptor<Log4jEventWrapper> disruptor;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private static volatile int count = 0;

    /**
     * Factory used to populate the RingBuffer with events. These event objects
     * are then re-used during the life of the RingBuffer.
     */
    private static final EventFactory<Log4jEventWrapper> FACTORY = new EventFactory<Log4jEventWrapper>() {
        @Override
        public Log4jEventWrapper newInstance() {
            return new Log4jEventWrapper();
        }
    };

    /**
     * Object responsible for passing on data to a specific RingBuffer event.
     */
    private final EventTranslator<Log4jEventWrapper> translator = new EventTranslator<Log4jEventWrapper>() {
        @Override
        public void translateTo(Log4jEventWrapper event, long sequence) {
            event.event = currentLogEvent.get();
            event.loggerConfig = asyncLoggerConfig;
        }
    };

    private ThreadLocal<LogEvent> currentLogEvent = new ThreadLocal<LogEvent>();
    private AsyncLoggerConfig asyncLoggerConfig;

    public AsyncLoggerConfigHelper(AsyncLoggerConfig asyncLoggerConfig) {
        this.asyncLoggerConfig = asyncLoggerConfig;
        initDisruptor();
    }

    private static synchronized void initDisruptor() {
        ++count;
        if (disruptor != null) {
            return;
        }
        int ringBufferSize = calculateRingBufferSize();
        WaitStrategy waitStrategy = createWaitStrategy();
        disruptor = new Disruptor<Log4jEventWrapper>(FACTORY,
                ringBufferSize, executor, ProducerType.MULTI, waitStrategy);
        EventHandler<Log4jEventWrapper>[] handlers = new Log4jEventWrapperHandler[] {//
                new Log4jEventWrapperHandler() };
        disruptor.handleExceptionsWith(getExceptionHandler());
        disruptor.handleEventsWith(handlers);

        LOGGER.debug(
                "Starting AsyncLoggerConfig disruptor with ringbuffer size {}...",
                disruptor.getRingBuffer().getBufferSize());
        disruptor.start();
    }

    private static WaitStrategy createWaitStrategy() {
        String strategy = System.getProperty("AsyncLoggerConfig.WaitStrategy");
        LOGGER.debug("property AsyncLoggerConfig.WaitStrategy={}", strategy);
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

    private static int calculateRingBufferSize() {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        String userPreferredRBSize = System.getProperty(
                "AsyncLoggerConfig.RingBufferSize",
                String.valueOf(ringBufferSize));
        try {
            int size = Integer.parseInt(userPreferredRBSize);
            if (size < RINGBUFFER_MIN_SIZE) {
                size = RINGBUFFER_MIN_SIZE;
                LOGGER.warn(
                        "Invalid RingBufferSize {}, using minimum size {}.",
                        userPreferredRBSize, RINGBUFFER_MIN_SIZE);
            }
            ringBufferSize = size;
        } catch (Exception ex) {
            LOGGER.warn("Invalid RingBufferSize {}, using default size {}.",
                    userPreferredRBSize, ringBufferSize);
        }
        return Util.ceilingNextPowerOfTwo(ringBufferSize);
    }

    private static ExceptionHandler getExceptionHandler() {
        String cls = System.getProperty("AsyncLoggerConfig.ExceptionHandler");
        if (cls == null) {
            LOGGER.debug("No AsyncLoggerConfig.ExceptionHandler specified");
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends ExceptionHandler> klass = (Class<? extends ExceptionHandler>) Class
                    .forName(cls);
            ExceptionHandler result = klass.newInstance();
            LOGGER.debug("AsyncLoggerConfig.ExceptionHandler=" + result);
            return result;
        } catch (Exception ignored) {
            LOGGER.debug(
                    "AsyncLoggerConfig.ExceptionHandler not set: error creating "
                            + cls + ": ", ignored);
            return null;
        }
    }

    /**
     * RingBuffer events contain all information necessary to perform the work
     * in a separate thread.
     */
    private static class Log4jEventWrapper {
        private AsyncLoggerConfig loggerConfig;
        private LogEvent event;
    }

    /**
     * EventHandler performs the work in a separate thread.
     */
    private static class Log4jEventWrapperHandler implements
            SequenceReportingEventHandler<Log4jEventWrapper> {
        private static final int NOTIFY_PROGRESS_THRESHOLD = 50;
        private Sequence sequenceCallback;
        private int counter;

        @Override
        public void setSequenceCallback(Sequence sequenceCallback) {
            this.sequenceCallback = sequenceCallback;
        }

        @Override
        public void onEvent(Log4jEventWrapper event, long sequence,
                boolean endOfBatch) throws Exception {
            event.event.setEndOfBatch(endOfBatch);
            event.loggerConfig.asyncCallAppenders(event.event);

            // notify the BatchEventProcessor that the sequence has progressed.
            // Without this callback the sequence would not be progressed
            // until the batch has completely finished.
            if (++counter > NOTIFY_PROGRESS_THRESHOLD) {
                sequenceCallback.set(sequence);
                counter = 0;
            }
        }
    }

    public synchronized void shutdown() {
        if (--count > 0) {
            return;
        }
        Disruptor<Log4jEventWrapper> temp = disruptor;
        if (temp == null) {
            return; // disruptor was already shut down by another thread
        }

        // Must guarantee that publishing to the RingBuffer has stopped
        // before we call disruptor.shutdown()
        disruptor = null; // client code fails with NPE if log after stop = OK
        temp.shutdown();

        // wait up to 10 seconds for the ringbuffer to drain
        RingBuffer<Log4jEventWrapper> ringBuffer = temp.getRingBuffer();
        for (int i = 0; i < MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN; i++) {
            if (ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize())) {
                break;
            }
            try {
                // give ringbuffer some time to drain...
                Thread.sleep(HALF_A_SECOND);
            } catch (InterruptedException e) {
                // ignored
            }
        }
        executor.shutdown(); // finally, kill the processor thread
    }

    public void callAppendersFromAnotherThread(LogEvent event) {
        currentLogEvent.set(event);
        disruptor.publishEvent(translator);
    }

}
