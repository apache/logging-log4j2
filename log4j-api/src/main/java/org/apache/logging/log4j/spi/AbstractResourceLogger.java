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
package org.apache.logging.log4j.spi;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Logger for resources. The Supplier provides resource attributes that should be included in all log events generated
 * from the current resource. The Supplier may be called when the Logger is created or for every log event.
 * Therefore, care should be taken to make the Supplier as efficient as possible to avoid performance issues.
 *
 * Unlike regular Loggers ResourceLoggers CANNOT be declared to be static. A ResourceLogger
 * must be declared as a class member that will be garbage collected along with the instance of the resource.
 */
public abstract class AbstractResourceLogger extends ExtendedLoggerWrapper {
    private static final long serialVersionUID = -2657891242014543083L;
    protected final Supplier<Map<String, ?>> supplier;
    protected final Map<String, ?> contextMap;

    /*
     * Pass our MessageFactory with its Supplier to AbstractLogger. This will be used to create
     * the Messages prior to them being passed to the "real" Logger.
     */
    protected AbstractResourceLogger(
            final ExtendedLogger logger, final Supplier<Map<String, ?>> supplier, MessageFactory messageFactory) {
        super(logger, logger.getName(), messageFactory);
        this.supplier = supplier;
        this.contextMap = null;
    }

    /*
     * Pass our MessageFactory with the Map created by the Supplier to AbstractLogger. This will be used to create
     * the Messages prior to them being passed to the "real" Logger.
     */
    protected AbstractResourceLogger(
            final ExtendedLogger logger, final Map<String, ?> map, MessageFactory messageFactory) {
        super(logger, logger.getName(), messageFactory);
        this.supplier = null;
        this.contextMap = map;
    }

    /**
     * Constructs a ResourceLogger.
     */
    protected abstract static class ResourceLoggerBuilder<B extends ResourceLoggerBuilder<B>> {
        protected static final Logger LOGGER = StatusLogger.getLogger();
        protected ExtendedLogger logger;
        protected String name;
        protected Supplier<Map<String, ?>> supplier;
        protected MessageFactory messageFactory;
        protected boolean supplyOnce;

        /**
         * Create the builder.
         */
        protected ResourceLoggerBuilder() {}

        /**
         * Add the underlying Logger to use. If a Logger, logger name, or class is not required
         * the name of the calling class wiill be used.
         * @param logger The Logger to use.
         * @return The ResourceLoggerBuilder.
         */
        public B withLogger(ExtendedLogger logger) {
            this.logger = logger;
            return asBuilder();
        }

        /**
         * Add the Logger name. If a Logger, logger name, or class is not required
         * the name of the calling class wiill be used.
         * @param name the name to assign to the Logger.
         * @return The ResourceLoggerBuilder.
         */
        public B withName(String name) {
            this.name = name;
            return asBuilder();
        }

        /**
         * The resource Class. If a Logger, logger name, or class is not required
         * the name of the calling class wiill be used.
         * @param clazz the resource Class.
         * @return the ResourceLoggerBuilder.
         */
        public B withClass(Class<?> clazz) {
            this.name = clazz.getCanonicalName() != null ? clazz.getCanonicalName() : clazz.getName();
            return asBuilder();
        }

        /**
         * The Map Supplier.
         * @param supplier the method that provides the Map of resource data to include in logs.
         * @return the ResourceLoggerBuilder.
         */
        public B withSupplier(Supplier<Map<String, ?>> supplier) {
            this.supplier = supplier;
            return asBuilder();
        }

        /**
         * Adds a MessageFactory.
         * @param messageFactory the MessageFactory to use to build messages. If a MessageFactory
         * is not specified the default MessageFactory will be used.
         * @return the ResourceLoggerBuilder.
         */
        public B withMessageFactory(MessageFactory messageFactory) {
            this.messageFactory = messageFactory;
            return asBuilder();
        }

        /**
         * Instruct the ResourceLogger to invoke the Supplier only once during Logger creation, if the
         * implementation supports it.
         * @return the ResourceLoggerBuilder.
         */
        public B supplyOnce() {
            this.supplyOnce = true;
            return asBuilder();
        }

        /**
         * Construct the ResourceLogger.
         * @return the ResourceLogger.
         */
        public abstract Logger build();

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }
    }
}
