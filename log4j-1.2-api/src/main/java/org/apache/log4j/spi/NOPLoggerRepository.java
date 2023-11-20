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
package org.apache.log4j.spi;

import java.util.Enumeration;
import java.util.Vector;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * No-operation implementation of LoggerRepository which is used when LogManager.repositorySelector is erroneously
 * nulled during class reloading.
 *
 * @since 1.2.15
 */
public final class NOPLoggerRepository implements LoggerRepository {

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHierarchyEventListener(final HierarchyEventListener listener) {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void emitNoAppenderWarning(final Category cat) {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger exists(final String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireAddAppenderEvent(final Category logger, final Appender appender) {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration getCurrentCategories() {
        return getCurrentLoggers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration getCurrentLoggers() {
        return new Vector<>().elements();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger(final String name) {
        return new NOPLogger(this, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger(final String name, final LoggerFactory factory) {
        return new NOPLogger(this, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getRootLogger() {
        return new NOPLogger(this, "root");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Level getThreshold() {
        return Level.OFF;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDisabled(final int level) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetConfiguration() {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThreshold(final Level level) {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThreshold(final String val) {
        // NOP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        // NOP
    }
}
