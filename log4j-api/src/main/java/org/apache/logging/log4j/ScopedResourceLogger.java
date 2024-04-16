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
import org.apache.logging.log4j.spi.AbstractResourceLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Logger for resources. The Supplier provides resource attributes that should be included in all log events generated
 * from the current resource. Note that since the Supplier is called for every LogEvent being generated
 * the values returned may change as necessary. Care should be taken to make the Supplier as efficient as
 * possible to avoid performance issues.
 *
 * Unlike regular Loggers ResourceLoggers CANNOT be declared to be static. A ResourceLogger
 * must be declared as a class member that will be garbage collected along with the instance of the resource.
 */
public final class ScopedResourceLogger extends AbstractResourceLogger {
    private static final long serialVersionUID = -5837924138744974513L;
    private final ScopedContext.Instance scopedInstance;

    public static ScopedResourceLoggerBuilder newBuilder() {
        return new ScopedResourceLoggerBuilder();
    }

    /*
     * Pass our MessageFactory with its Supplier to AbstractLogger. This will be used to create
     * the Messages prior to them being passed to the "real" Logger.
     */
    private ScopedResourceLogger(
            final ExtendedLogger logger, final Supplier<Map<String, ?>> supplier, MessageFactory messageFactory) {
        super(logger, supplier, messageFactory);
        scopedInstance = null;
    }

    /*
     * Pass our MessageFactory with its Map to AbstractLogger. This will be used to create
     * the Messages prior to them being passed to the "real" Logger.
     */
    private ScopedResourceLogger(final ExtendedLogger logger, final Map<String, ?> map, MessageFactory messageFactory) {
        super(logger, map, messageFactory);
        scopedInstance = ScopedContext.where(map);
    }

    @Override
    public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
        if (supplier != null) {
            ScopedContext.runWhere(supplier.get(), () -> logger.logMessage(fqcn, level, marker, message, t));
        } else if (scopedInstance != null) {
            scopedInstance.run(() -> logger.logMessage(fqcn, level, marker, message, t));
        } else {
            logger.logMessage(fqcn, level, marker, message, t);
        }
    }

    /**
     * Constructs a ResourceLogger.
     */
    public static final class ScopedResourceLoggerBuilder extends ResourceLoggerBuilder<ScopedResourceLoggerBuilder> {

        /**
         * Create the builder.
         */
        private ScopedResourceLoggerBuilder() {
            super();
        }

        /**
         * Construct the ResourceLogger.
         * @return the ResourceLogger.
         */
        public Logger build() {
            if (this.logger == null) {
                if (Strings.isEmpty(name)) {
                    Class<?> clazz = StackLocatorUtil.getCallerClass(2);
                    name = clazz.getCanonicalName() != null ? clazz.getCanonicalName() : clazz.getName();
                }
                this.logger = (ExtendedLogger) LogManager.getLogger(name);
            }
            if (supplyOnce) {
                Map<String, ?> map = this.supplier != null ? supplier.get() : Collections.emptyMap();
                return new ScopedResourceLogger(logger, map, messageFactory);
            } else {
                Supplier<Map<String, ?>> mapSupplier = this.supplier != null ? this.supplier : Collections::emptyMap;
                return new ScopedResourceLogger(logger, mapSupplier, messageFactory);
            }
        }
    }
}
