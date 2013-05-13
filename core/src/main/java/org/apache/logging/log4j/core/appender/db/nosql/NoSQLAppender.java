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
package org.apache.logging.log4j.core.appender.db.nosql;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * This Appender writes logging events to a NoSQL database using a configured NoSQL provider. It requires
 * implementations of {@link NoSQLObject}, {@link NoSQLConnection}, and {@link NoSQLProvider} to "know" how to write
 * events to the chosen NoSQL database. Two provider implementations are provided: MongoDB
 * (org.mongodb:mongo-java-driver:2.11.1 or newer must be on the classpath) and Apache CouchDB
 * (org.lightcouch:lightcouch:0.0.5 or newer must be on the classpath). For examples on how to write your own NoSQL
 * provider, see the simple source code for the MongoDB and CouchDB providers.
 * 
 * @see NoSQLObject
 * @see NoSQLConnection
 * @see NoSQLProvider
 */
@Plugin(name = "NoSql", category = "Core", elementType = "appender", printObject = true)
public final class NoSQLAppender extends AbstractDatabaseAppender<NoSQLDatabaseManager<?>> {
    private final String description;

    private NoSQLAppender(final String name, final Filter filter, final boolean handleException,
                          final NoSQLDatabaseManager<?> manager) {
        super(name, filter, handleException, manager);
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
     * @param suppressExceptions {@code "true"} (default) if logging exceptions should be hidden from the application,
     *                           {@code "false"} otherwise.
     * @param filter The filter, if any, to use.
     * @param bufferSize If an integer greater than 0, this causes the appender to buffer log events and flush whenever
     *                   the buffer reaches this size.
     * @param provider The NoSQL provider that provides connections to the chosen NoSQL database.
     * @return a new NoSQL appender.
     */
    @PluginFactory
    public static NoSQLAppender createAppender(@PluginAttr("name") final String name,
                                               @PluginAttr("suppressExceptions") final String suppressExceptions,
                                               @PluginElement("filter") final Filter filter,
                                               @PluginAttr("bufferSize") final String bufferSize,
                                               @PluginElement("noSqlProvider") final NoSQLProvider<?> provider) {
        if (provider == null) {
            LOGGER.error("NoSQL provider not specified for appender [{}].", name);
            return null;
        }

        int bufferSizeInt;
        try {
            bufferSizeInt = bufferSize == null || bufferSize.length() == 0 ? 0 : Integer.parseInt(bufferSize);
        } catch (final NumberFormatException e) {
            LOGGER.warn("Buffer size [" + bufferSize + "] not an integer, using no buffer.");
            bufferSizeInt = 0;
        }

        final boolean handleExceptions = suppressExceptions == null || !Boolean.parseBoolean(suppressExceptions);

        final String managerName = "noSqlManager{ description=" + name + ", bufferSize=" + bufferSizeInt
                + ", provider=" + provider + " }";

        final NoSQLDatabaseManager<?> manager = NoSQLDatabaseManager.getNoSQLDatabaseManager(
                managerName, bufferSizeInt, provider
        );
        if (manager == null) {
            return null;
        }

        return new NoSQLAppender(name, filter, handleExceptions, manager);
    }
}
