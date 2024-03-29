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
package org.apache.logging.log4j;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Logger for resources. Formats all events using the ParameterizedMapMessageFactory along with the provided
 * Supplier. The Supplier provides resource attributes that should be included in all log events generated
 * from the current resource. Note that since the Supplier is called for every LogEvent being generated
 * the values returned may change as necessary. Care should be taken to make the Supplier as efficient as
 * possible to avoid performance issues.
 *
 * Unlike regular Loggers ResourceLoggers CANNOT be declared to be static. A ResourceLogger
 * must be declared as a class member that will be garbage collected along with the instance of the resource.
 */
public final class ResourceLogger extends ExtendedLoggerWrapper {
    private static final long serialVersionUID = -5837924138744974513L;
    private final Supplier<Map<String, ?>> supplier;

    public static ResourceLoggerBuilder newBuilder() {
        return new ResourceLoggerBuilder();
    }

    /*
     * Pass our MessageFactory with its Supplier to AbstractLogger. This will be used to create
     * the Messages prior to them being passed to the "real" Logger.
     */
    private ResourceLogger(
            final ExtendedLogger logger, final Supplier<Map<String, ?>> supplier, MessageFactory messageFactory) {
        super(logger, logger.getName(), messageFactory);
        this.supplier = supplier;
    }

    @Override
    public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
        if (supplier != null) {
            ScopedContext.runWhere(supplier.get(), () -> logger.logMessage(fqcn, level, marker, message, t));
        } else {
            logger.logMessage(fqcn, level, marker, message, t);
        }
    }

    /**
     * Constructs a ResourceLogger.
     */
    public static final class ResourceLoggerBuilder {
        private static final Logger LOGGER = StatusLogger.getLogger();
        private ExtendedLogger logger;
        private String name;
        private Supplier<Map<String, ?>> supplier;
        private MessageFactory messageFactory;

        /**
         * Create the builder.
         */
        private ResourceLoggerBuilder() {}

        /**
         * Add the underlying Logger to use. If a Logger, logger name, or class is not required
         * the name of the calling class wiill be used.
         * @param logger The Logger to use.
         * @return The ResourceLoggerBuilder.
         */
        public ResourceLoggerBuilder withLogger(ExtendedLogger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Add the Logger name. If a Logger, logger name, or class is not required
         * the name of the calling class wiill be used.
         * @param name the name to assign to the Logger.
         * @return The ResourceLoggerBuilder.
         */
        public ResourceLoggerBuilder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * The resource Class. If a Logger, logger name, or class is not required
         * the name of the calling class wiill be used.
         * @param clazz the resource Class.
         * @return the ResourceLoggerBuilder.
         */
        public ResourceLoggerBuilder withClass(Class<?> clazz) {
            this.name = clazz.getCanonicalName() != null ? clazz.getCanonicalName() : clazz.getName();
            return this;
        }

        /**
         * The Map Supplier.
         * @param supplier the method that provides the Map of resource data to include in logs.
         * @return the ResourceLoggerBuilder.
         */
        public ResourceLoggerBuilder withSupplier(Supplier<Map<String, ?>> supplier) {
            this.supplier = supplier;
            return this;
        }

        /**
         * Adds a MessageFactory.
         * @param messageFactory the MessageFactory to use to build messages. If a MessageFactory
         * is not specified the default MessageFactory will be used.
         * @return the ResourceLoggerBuilder.
         */
        public ResourceLoggerBuilder withMessageFactory(MessageFactory messageFactory) {
            this.messageFactory = messageFactory;
            return this;
        }

        /**
         * Construct the ResourceLogger.
         * @return the ResourceLogger.
         */
        public ResourceLogger build() {
            if (this.logger == null) {
                if (Strings.isEmpty(name)) {
                    Class<?> clazz = StackLocatorUtil.getCallerClass(2);
                    name = clazz.getCanonicalName() != null ? clazz.getCanonicalName() : clazz.getName();
                }
                this.logger = (ExtendedLogger) LogManager.getLogger(name);
            }
            Supplier<Map<String, ?>> mapSupplier = this.supplier != null ? this.supplier : Collections::emptyMap;
            return new ResourceLogger(logger, mapSupplier, messageFactory);
        }
    }
}
