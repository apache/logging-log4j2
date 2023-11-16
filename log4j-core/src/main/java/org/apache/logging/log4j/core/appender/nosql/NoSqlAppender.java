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
package org.apache.logging.log4j.core.appender.nosql;

import java.io.Serializable;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.KeyValuePair;

/**
 * This Appender writes logging events to a NoSQL database using a configured NoSQL provider. It requires
 * implementations of {@link NoSqlObject}, {@link NoSqlConnection}, and {@link NoSqlProvider} to "know" how to write
 * events to the chosen NoSQL database.
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

    /**
     * Builds ConsoleAppender instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<NoSqlAppender> {

        @PluginBuilderAttribute("bufferSize")
        private int bufferSize;

        @PluginElement("NoSqlProvider")
        private NoSqlProvider<?> provider;

        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;

        @SuppressWarnings("resource")
        @Override
        public NoSqlAppender build() {
            final String name = getName();
            if (provider == null) {
                LOGGER.error("NoSQL provider not specified for appender [{}].", name);
                return null;
            }

            final String managerName = "noSqlManager{ description=" + name + ", bufferSize=" + bufferSize
                    + ", provider=" + provider + " }";
            final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager(
                    managerName, bufferSize, provider, additionalFields, getConfiguration());
            if (manager == null) {
                return null;
            }

            return new NoSqlAppender(name, getFilter(), getLayout(), isIgnoreExceptions(), getPropertyArray(), manager);
        }

        /**
         * Sets the buffer size.
         *
         * @param bufferSize
         *            If an integer greater than 0, this causes the appender to buffer log events and flush whenever the
         *            buffer reaches this size.
         * @return this
         */
        public B setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return asBuilder();
        }

        /**
         * Sets the provider.
         *
         * @param provider
         *            The NoSQL provider that provides connections to the chosen NoSQL database.
         * @return this
         */
        public B setProvider(final NoSqlProvider<?> provider) {
            this.provider = provider;
            return asBuilder();
        }
    }

    /**
     * Factory method for creating a NoSQL appender within the plugin manager.
     *
     * @param name
     *            The name of the appender.
     * @param ignore
     *            If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @param filter
     *            The filter, if any, to use.
     * @param bufferSize
     *            If an integer greater than 0, this causes the appender to buffer log events and flush whenever the
     *            buffer reaches this size.
     * @param provider
     *            The NoSQL provider that provides connections to the chosen NoSQL database.
     * @return a new NoSQL appender.
     * @deprecated since 2.11.0; use {@link Builder}.
     */
    @SuppressWarnings("resource")
    @Deprecated
    public static NoSqlAppender createAppender(
            // @formatter:off
            final String name,
            final String ignore,
            final Filter filter,
            final String bufferSize,
            final NoSqlProvider<?> provider) {
        // @formatter:on
        if (provider == null) {
            LOGGER.error("NoSQL provider not specified for appender [{}].", name);
            return null;
        }

        final int bufferSizeInt = AbstractAppender.parseInt(bufferSize, 0);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        final String managerName =
                "noSqlManager{ description=" + name + ", bufferSize=" + bufferSizeInt + ", provider=" + provider + " }";

        final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager(managerName, bufferSizeInt, provider, null, null);
        if (manager == null) {
            return null;
        }

        return new NoSqlAppender(name, filter, null, ignoreExceptions, null, manager);
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String description;

    private NoSqlAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final Property[] properties,
            final NoSqlDatabaseManager<?> manager) {
        super(name, filter, layout, ignoreExceptions, properties, manager);
        this.description = this.getName() + "{ manager=" + this.getManager() + " }";
    }

    @Override
    public String toString() {
        return this.description;
    }
}
