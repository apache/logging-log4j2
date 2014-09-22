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
package org.apache.log4j.spi;

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A <code>LoggerRepository</code> is used to create and retrieve <code>Loggers</code>.
 * <p>
 * The relation between loggers in a repository depends on the repository but typically loggers are arranged in a named
 * hierarchy.
 * </p>
 * <p>
 * In addition to the creational methods, a <code>LoggerRepository</code> can be queried for existing loggers, can act
 * as a point of registry for events related to loggers.
 * </p>
 *
 * @since 1.2
 */
public interface LoggerRepository {

    /**
     * Add a {@link HierarchyEventListener} event to the repository.
     *
     * @param listener The listener
     */
    void addHierarchyEventListener(HierarchyEventListener listener);

    /**
     * Returns whether this repository is disabled for a given
     * level. The answer depends on the repository threshold and the
     * <code>level</code> parameter. See also {@link #setThreshold}
     * method.
     *
     * @param level The level
     * @return whether this repository is disabled.
     */
    boolean isDisabled(int level);

    /**
     * Set the repository-wide threshold. All logging requests below the
     * threshold are immediately dropped. By default, the threshold is
     * set to <code>Level.ALL</code> which has the lowest possible rank.
     *
     * @param level The level
     */
    void setThreshold(Level level);

    /**
     * Another form of {@link #setThreshold(Level)} accepting a string
     * parameter instead of a <code>Level</code>.
     *
     * @param val The threshold value
     */
    void setThreshold(String val);

    void emitNoAppenderWarning(Category cat);

    /**
     * Get the repository-wide threshold. See {@link #setThreshold(Level)} for an explanation.
     *
     * @return the level.
     */
    Level getThreshold();

    Logger getLogger(String name);

    Logger getLogger(String name, LoggerFactory factory);

    Logger getRootLogger();

    Logger exists(String name);

    void shutdown();

    @SuppressWarnings("rawtypes")
    Enumeration getCurrentLoggers();

    /**
     * Deprecated. Please use {@link #getCurrentLoggers} instead.
     *
     * @return an enumeration of loggers.
     */
    @SuppressWarnings("rawtypes")
    Enumeration getCurrentCategories();

    void fireAddAppenderEvent(Category logger, Appender appender);

    void resetConfiguration();
}
