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
package org.apache.logging.log4j.nosql.appender;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * This Appender writes logging events to a NoSQL database using a configured NoSQL provider. It requires
 * implementations of {@link NoSqlObject}, {@link NoSqlConnection}, and {@link NoSqlProvider} to "know" how to write
 * events to the chosen NoSQL database.
 * 
 * <p>
 * Two provider implementations are provided:
 * </p>
 * <ul>
 * <li>
 * MongoDB (org.mongodb:mongo-java-driver:2.11.1 or newer must be on the classpath)</li>
 * <li>
 * Apache CouchDB (org.lightcouch:lightcouch:0.0.5 or newer must be on the classpath).</li>
 * </ul>
 * <p>
 * For examples on how to write your own NoSQL provider, see the simple source code for the MongoDB and CouchDB
 * providers.
 * </p>
 * 
 * @see NoSqlObject
 * @see NoSqlConnection
 * @see NoSqlProvider
 */
@Plugin(name = "NoSql", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class NoSqlAppender extends AbstractDatabaseAppender<NoSqlDatabaseManager<?>> {
    private final String description;

    private NoSqlAppender(final String name, final Filter filter, final boolean ignoreExceptions,
                          final NoSqlDatabaseManager<?> manager) {
        super(name, filter, ignoreExceptions, manager);
        this.description = this.getName() + "{ manager=" + this.getManager() + " }";
    }

    @Override
    public String toString() {
        return this.description;
    }

    /**
     * Factory method for creating a NoSQL appender within the plugin manager.
     *
     * @param name The name of the appender.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param filter The filter, if any, to use.
     * @param bufferSize If an integer greater than 0, this causes the appender to buffer log events and flush whenever
     *                   the buffer reaches this size.
     * @param provider The NoSQL provider that provides connections to the chosen NoSQL database.
     * @return a new NoSQL appender.
     */
    @PluginFactory
    public static NoSqlAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("bufferSize") final String bufferSize,
            @PluginElement("NoSqlProvider") final NoSqlProvider<?> provider) {
        if (provider == null) {
            LOGGER.error("NoSQL provider not specified for appender [{}].", name);
            return null;
        }

        final int bufferSizeInt = AbstractAppender.parseInt(bufferSize, 0);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        final String managerName = "noSqlManager{ description=" + name + ", bufferSize=" + bufferSizeInt
                + ", provider=" + provider + " }";

        final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager(
                managerName, bufferSizeInt, provider
        );
        if (manager == null) {
            return null;
        }

        return new NoSqlAppender(name, filter, ignoreExceptions, manager);
    }
}
