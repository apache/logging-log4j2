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
import org.apache.logging.log4j.message.ParameterizedMapMessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;
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
public final class ResourceLogger extends AbstractLogger {
    private static final long serialVersionUID = -5837924138744974513L;
    private final ExtendedLogger logger;

    public static ResourceLoggerBuilder newBuilder() {
        return new ResourceLoggerBuilder();
    }

    /*
     * Pass our MessageFactory with its Supplier to AbstractLogger. This will be used to create
     * the Messages prior to them being passed to the "real" Logger.
     */
    private ResourceLogger(final ExtendedLogger logger, final Supplier<Map<String, String>> supplier) {
        super(logger.getName(), new ParameterizedMapMessageFactory(supplier));
        this.logger = logger;
    }

    @Override
    public Level getLevel() {
        return logger.getLevel();
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Message message, Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, CharSequence message, Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Object message, Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Throwable t) {
        return logger.isEnabled(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message) {
        return logger.isEnabled(level, marker, message);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object... params) {
        return logger.isEnabled(level, marker, message, params);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0) {
        return logger.isEnabled(level, marker, message, p0);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1) {
        return logger.isEnabled(level, marker, message, p0, p1);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
        return logger.isEnabled(level, marker, message, p0, p1, p2);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3);
    }

    @Override
    public boolean isEnabled(
            Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public boolean isEnabled(
            Level level,
            Marker marker,
            String message,
            Object p0,
            Object p1,
            Object p2,
            Object p3,
            Object p4,
            Object p5,
            Object p6,
            Object p7,
            Object p8,
            Object p9) {
        return logger.isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
        logger.logMessage(fqcn, level, marker, message, t);
    }

    /**
     * Constructs a ResourceLogger.
     */
    public static final class ResourceLoggerBuilder {
        private static final Logger LOGGER = StatusLogger.getLogger();
        private ExtendedLogger logger;
        private String name;
        private Supplier<Map<String, String>> supplier;

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
        public ResourceLoggerBuilder withSupplier(Supplier<Map<String, String>> supplier) {
            this.supplier = supplier;
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
            Supplier<Map<String, String>> mapSupplier = this.supplier != null ? this.supplier : Collections::emptyMap;
            return new ResourceLogger(logger, mapSupplier);
        }
    }
}
