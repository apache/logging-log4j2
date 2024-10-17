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
package org.apache.logging.jul.tolog4j.spi;

import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Abstract Logger registry.
 * <p>
 *     JUL contains methods, such as {@link Logger#setLevel}, which modify the configuration of the logging backend.
 *     To fully implement all {@code Logger} methods, we need to provide a different {@code Logger} implementation
 *     for each Log4j API implementation.
 * </p>
 * <p>
 *     Older Log4j versions provided an alternative {@code CoreLoggerAdapter} implementation that supported
 *     the modification of Log4j Core configuration using JUL.
 * </p>
 *     Since version 2.24.0, however, this implementation was deprecated for removal.
 *     If you wish to enable this feature again, you need to implement this class and provide its FQCN
 *     as {@code log4j.jul.loggerAdapter} configuration property.
 * </p>
 * <p>
 *     <strong>Implementation note:</strong> since version 3.0.0, this interface was moved to a new package.
 * </p>
 *
 * @see <a href="https://github.com/apache/logging-log4j2/issues/2353">Issue #2353</a>
 * @since 2.1
 */
public abstract class AbstractLoggerAdapter extends org.apache.logging.log4j.spi.AbstractLoggerAdapter<Logger> {

    /**
     * Creates a new {@link java.util.logging.Logger}
     * <p>
     *     Each implementation should provide this method.
     * </p>
     */
    @Override
    public abstract Logger newLogger(String name, LoggerContext context);

    /**
     * Provides the most appropriate {@link LoggerContext} for the caller.
     */
    @Override
    public LoggerContext getContext() {
        return getContext(
                LogManager.getFactory().isClassLoaderDependent()
                        ? StackLocatorUtil.getCallerClass(java.util.logging.LogManager.class)
                        : null);
    }
}
