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
package org.apache.logging.jul.tolog4j.internal;

import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.jul.tolog4j.support.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Implementation of {@link java.util.logging.Logger} that ignores all method calls that do not have an equivalent in
 * the Log4j API.
 */
public class ApiLogger extends AbstractLogger {

    private static final String MUTATOR_DISABLED =
            """
            Ignoring call to `j.ul.Logger.{}()`, since the Log4j API does not provide methods to modify the underlying implementation.
            To modify the configuration using JUL, use an `AbstractLoggerAdapter` appropriate for your logging implementation.
            See https://logging.apache.org/log4j/3.x/log4j-jul.html#log4j.jul.loggerAdapter for more information.""";
    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    public ApiLogger(ExtendedLogger logger) {
        super(logger);
    }

    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
        LOGGER.warn(MUTATOR_DISABLED, "setFilter");
    }

    @Override
    public void setLevel(Level newLevel) throws SecurityException {
        LOGGER.warn(MUTATOR_DISABLED, "setLevel");
    }

    @Override
    public void addHandler(Handler handler) throws SecurityException {
        LOGGER.warn(MUTATOR_DISABLED, "addHandler");
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        LOGGER.warn(MUTATOR_DISABLED, "removeHandler");
    }

    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
        LOGGER.warn(MUTATOR_DISABLED, "setUseParentHandlers");
    }

    @Override
    public void setParent(Logger parent) {
        throw new UnsupportedOperationException(
                ApiLogger.class.getSimpleName() + " does not support `j.u.l.Logger#setParent()`.");
    }
}
