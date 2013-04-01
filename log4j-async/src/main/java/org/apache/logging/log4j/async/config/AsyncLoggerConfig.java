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
package org.apache.logging.log4j.async.config;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerContext.Status;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.Util;

/**
 * Asynchronous Logger object that is created via configuration.
 */
@Plugin(name = "asyncLogger", type = "Core", printObject = true)
public class AsyncLoggerConfig extends LoggerConfig {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static volatile Disruptor<RingBufferLog4jEvent> disruptor;
    private static ExecutorService executor = Executors
            .newSingleThreadExecutor();

    private ThreadLocal<LogEvent> currentLogEvent = new ThreadLocal<LogEvent>();

    /**
     * RingBuffer events contain all information necessary to perform the work
     * in a separate thread.
     */
    private static class RingBufferLog4jEvent {
        private AsyncLoggerConfig loggerConfig;
        private LogEvent event;
    }

    /**
     * Factory used to populate the RingBuffer with events. These event objects
     * are then re-used during the life of the RingBuffer.
     */
    private static final EventFactory<RingBufferLog4jEvent> FACTORY = new EventFactory<RingBufferLog4jEvent>() {
        @Override
        public RingBufferLog4jEvent newInstance() {
            return new RingBufferLog4jEvent();
        }
    };

    /**
     * Object responsible for passing on data to a specific RingBuffer event.
     */
    private final EventTranslator<RingBufferLog4jEvent> translator = new EventTranslator<RingBufferLog4jEvent>() {
        @Override
        public void translateTo(RingBufferLog4jEvent event, long sequence) {
            event.event = currentLogEvent.get();
            event.loggerConfig = AsyncLoggerConfig.this;
        }
    };

    /**
     * EventHandler performs the work in a separate thread.
     */
    private static class RingBufferLog4jEventHandler implements
            EventHandler<RingBufferLog4jEvent> {
        @Override
        public void onEvent(RingBufferLog4jEvent event, long sequence,
                boolean endOfBatch) throws Exception {
            event.loggerConfig.asyncCallAppenders(event.event, endOfBatch);
        }
    }

    /**
     * Default constructor.
     */
    public AsyncLoggerConfig() {
        super();
    }

    /**
     * Constructor that sets the name, level and additive values.
     * 
     * @param name The Logger name.
     * @param level The Level.
     * @param additive true if the Logger is additive, false otherwise.
     */
    public AsyncLoggerConfig(final String name, final Level level,
            final boolean additive) {
        super(name, level, additive);
    }

    protected AsyncLoggerConfig(final String name,
            final List<AppenderRef> appenders, final Filter filter,
            final Level level, final boolean additive,
            final Property[] properties, final Configuration config,
            final boolean includeLocation) {
        super(name, appenders, filter, level, additive, properties, config,
                includeLocation);
    }

    /**
     * Passes on the event to a separate thread that will call
     * {@link #asyncCallAppenders(LogEvent)}.
     */
    @Override
    protected void callAppenders(LogEvent event) {
        // populate lazily initialized fields
        event.getSource();
        event.getThreadName();

        // pass on the event to a separate thread
        currentLogEvent.set(event);
        disruptor.publishEvent(translator);
    }

    /** Called by RingBufferLog4jEventHandler. */
    private void asyncCallAppenders(LogEvent event, boolean endOfBatch) {
        event.setEndOfBatch(endOfBatch);
        super.callAppenders(event);
    }

    @Override
    public void startFilter() {
        if (disruptor == null) {
            int ringBufferSize = calculateRingBufferSize();
            WaitStrategy waitStrategy = createWaitStrategy();
            disruptor = new Disruptor<RingBufferLog4jEvent>(FACTORY,
                    ringBufferSize, executor, ProducerType.MULTI, waitStrategy);
            EventHandler<RingBufferLog4jEvent>[] handlers = new RingBufferLog4jEventHandler[] { new RingBufferLog4jEventHandler() };
            disruptor.handleExceptionsWith(getExceptionHandler());
            disruptor.handleEventsWith(handlers);

            LOGGER.debug(
                    "Starting AsyncLoggerConfig disruptor with ringbuffer size {}...",
                    disruptor.getRingBuffer().getBufferSize());
            disruptor.start();
        }
        super.startFilter();
    }

    private WaitStrategy createWaitStrategy() {
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
        String userPreferredRBSize = System.getProperty(
                "AsyncLoggerConfig.RingBufferSize", "256000");
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

    @Override
    public void stopFilter() {
        // only stop disruptor if shutting down logging subsystem
        if (LogManager.getContext() instanceof LoggerContext) {
            if (((LoggerContext) LogManager.getContext()).getStatus() != Status.STOPPING) {
                return;
            }
        }
        Disruptor<RingBufferLog4jEvent> temp = disruptor;

        // Must guarantee that publishing to the RingBuffer has stopped
        // before we call disruptor.shutdown()
        disruptor = null; // client code fails with NPE if log after stop = OK
        temp.shutdown();

        // wait up to 10 seconds for the ringbuffer to drain
        RingBuffer<RingBufferLog4jEvent> ringBuffer = temp.getRingBuffer();
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
        super.stopFilter();
    }

    /**
     * Factory method to create a LoggerConfig.
     * 
     * @param additivity True if additive, false otherwise.
     * @param levelName The Level to be associated with the Logger.
     * @param loggerName The name of the Logger.
     * @param includeLocation "true" if location should be passed downstream
     * @param refs An array of Appender names.
     * @param properties Properties to pass to the Logger.
     * @param config The Configuration.
     * @param filter A Filter.
     * @return A new LoggerConfig.
     */
    @PluginFactory
    public static LoggerConfig createLogger(
            @PluginAttr("additivity") final String additivity,
            @PluginAttr("level") final String levelName,
            @PluginAttr("name") final String loggerName,
            @PluginAttr("includeLocation") final String includeLocation,
            @PluginElement("appender-ref") final AppenderRef[] refs,
            @PluginElement("properties") final Property[] properties,
            @PluginConfiguration final Configuration config,
            @PluginElement("filters") final Filter filter) {
        if (loggerName == null) {
            LOGGER.error("Loggers cannot be configured without a name");
            return null;
        }

        final List<AppenderRef> appenderRefs = Arrays.asList(refs);
        Level level;
        try {
            level = Level.toLevel(levelName, Level.ERROR);
        } catch (final Exception ex) {
            LOGGER.error(
                    "Invalid Log level specified: {}. Defaulting to Error",
                    levelName);
            level = Level.ERROR;
        }
        final String name = loggerName.equals("root") ? "" : loggerName;
        final boolean additive = additivity == null ? true : Boolean
                .parseBoolean(additivity);

        return new AsyncLoggerConfig(name, appenderRefs, filter, level,
                additive, properties, config, includeLocation(includeLocation));
    }
    
    // Note: for asynchronous loggers, includeLocation default is FALSE
    private static boolean includeLocation(String includeLocationConfigValue) {
        if (includeLocationConfigValue == null) {
            return false;
        }
        return Boolean.parseBoolean(includeLocationConfigValue);
    }

    /**
     * An asynchronous root Logger.
     */
    @Plugin(name = "asyncRoot", type = "Core", printObject = true)
    public static class RootLogger extends LoggerConfig {

        @PluginFactory
        public static LoggerConfig createLogger(
                @PluginAttr("additivity") final String additivity,
                @PluginAttr("level") final String levelName,
                @PluginAttr("includeLocation") final String includeLocation,
                @PluginElement("appender-ref") final AppenderRef[] refs,
                @PluginElement("properties") final Property[] properties,
                @PluginConfiguration final Configuration config,
                @PluginElement("filters") final Filter filter) {
            final List<AppenderRef> appenderRefs = Arrays.asList(refs);
            Level level;
            try {
                level = Level.toLevel(levelName, Level.ERROR);
            } catch (final Exception ex) {
                LOGGER.error(
                        "Invalid Log level specified: {}. Defaulting to Error",
                        levelName);
                level = Level.ERROR;
            }
            final boolean additive = additivity == null ? true : Boolean
                    .parseBoolean(additivity);

            return new AsyncLoggerConfig(LogManager.ROOT_LOGGER_NAME,
                    appenderRefs, filter, level, additive, properties, config,
                    includeLocation(includeLocation));
        }
    }
}
