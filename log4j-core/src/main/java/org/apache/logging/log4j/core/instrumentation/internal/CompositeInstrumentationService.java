/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.instrumentation.internal;

import java.util.ServiceLoader;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.instrumentation.InstrumentationService;
import org.apache.logging.log4j.core.jmx.RingBufferAdminMBean;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

public final class CompositeInstrumentationService implements InstrumentationService {

    private static final Lazy<InstrumentationService> INSTANCE =
            Lazy.lazy(CompositeInstrumentationService::createInstance);

    private static InstrumentationService createInstance() {
        final InstrumentationService[] services = ServiceLoaderUtil.safeStream(
                        InstrumentationService.class,
                        ServiceLoader.load(InstrumentationService.class, Loader.getClassLoader()),
                        StatusLogger.getLogger())
                .toArray(InstrumentationService[]::new);
        return new CompositeInstrumentationService(services);
    }

    public static InstrumentationService getInstance() {
        return INSTANCE.get();
    }

    private final InstrumentationService[] services;

    private CompositeInstrumentationService(final InstrumentationService[] services) {
        this.services = services;
    }

    @Override
    public MessageFactory instrumentMessageFactory(final String loggerName, final MessageFactory messageFactory) {
        MessageFactory result = messageFactory;
        for (final InstrumentationService service : services) {
            result = service.instrumentMessageFactory(loggerName, result);
        }
        return result;
    }

    @Override
    public ReliabilityStrategy instrumentReliabilityStrategy(
            final String loggerName, final ReliabilityStrategy strategy) {
        ReliabilityStrategy result = strategy;
        for (final InstrumentationService service : services) {
            result = service.instrumentReliabilityStrategy(loggerName, result);
        }
        return result;
    }

    @Override
    public LogEventFactory instrumentLogEventFactory(final String loggerName, final LogEventFactory logEventFactory) {
        LogEventFactory result = logEventFactory;
        for (final InstrumentationService service : services) {
            result = service.instrumentLogEventFactory(loggerName, result);
        }
        return result;
    }

    @Override
    public AsyncQueueFullPolicy instrumentQueueFullPolicy(
            final String parentType, final String parentName, final AsyncQueueFullPolicy queueFullPolicy) {
        AsyncQueueFullPolicy result = queueFullPolicy;
        for (final InstrumentationService service : services) {
            result = service.instrumentQueueFullPolicy(parentType, parentName, result);
        }
        return result;
    }

    @Override
    public void instrumentRingBuffer(final RingBufferAdminMBean ringBufferAdmin) {
        for (final InstrumentationService service : services) {
            service.instrumentRingBuffer(ringBufferAdmin);
        }
    }

    @Override
    public LoggerConfig instrumentLoggerConfig(final LoggerConfig loggerConfig) {
        LoggerConfig result = loggerConfig;
        for (final InstrumentationService service : services) {
            result = service.instrumentLoggerConfig(result);
        }
        return result;
    }

    @Override
    public Appender instrumentAppender(final Appender appender) {
        Appender result = appender;
        for (final InstrumentationService service : services) {
            result = service.instrumentAppender(result);
        }
        return result;
    }
}
