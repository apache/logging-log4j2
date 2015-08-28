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

package org.apache.logging.log4j.jul;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log4j Core implementation of the JUL {@link Logger} class. <strong>Note that this implementation does
 * <em>not</em> use the {@link java.util.logging.Handler} class.</strong> Instead, logging is delegated to the
 * underlying Log4j {@link org.apache.logging.log4j.core.Logger} which uses
 * {@link org.apache.logging.log4j.core.Appender Appenders} instead.
 *
 * @since 2.1
 */
public class CoreLogger extends ApiLogger {

    private final org.apache.logging.log4j.core.Logger logger;

    /**
     * Constructs a Logger using a Log4j {@link org.apache.logging.log4j.core.Logger}.
     *
     * @param logger the underlying Logger to base this Logger on
     */
    CoreLogger(final org.apache.logging.log4j.core.Logger logger) {
        super(logger);
        this.logger = logger;
    }

    @Override
    public void setLevel(final Level level) throws SecurityException {
        super.doSetLevel(level); // checks permissions
        logger.setLevel(LevelTranslator.toLevel(level));
    }

    /**
     * Marks the underlying {@link org.apache.logging.log4j.core.Logger} as additive.
     *
     * @param additive {@code true} if this Logger should be additive
     * @see org.apache.logging.log4j.core.Logger#setAdditive(boolean)
     */
    @Override
    public synchronized void setUseParentHandlers(final boolean additive) {
        logger.setAdditive(additive);
    }

    /**
     * Indicates if the underlying {@link org.apache.logging.log4j.core.Logger} is additive. <strong>Note that the
     * Log4j version of JDK Loggers do <em>not</em> use Handlers.</strong>
     *
     * @return {@code true} if this Logger is additive, or {@code false} otherwise
     * @see org.apache.logging.log4j.core.Logger#isAdditive()
     */
    @Override
    public synchronized boolean getUseParentHandlers() {
        return logger.isAdditive();
    }

    @Override
    public Logger getParent() {
        final org.apache.logging.log4j.core.Logger parent = logger.getParent();
        return parent == null ? null : Logger.getLogger(parent.getName());
    }
}
