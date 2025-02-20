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
package org.apache.logging.log4j.mongodb4;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.nosql.NoSqlProvider;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.status.StatusLogger;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * The MongoDB implementation of {@link NoSqlProvider} using the MongoDB driver
 * version 4 API.
 */
@Plugin(name = MongoDb4Provider.PLUGIN_NAME, category = Core.CATEGORY_NAME, printObject = true)
public final class MongoDb4Provider implements NoSqlProvider<MongoDb4Connection> {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    static final String PLUGIN_NAME = "MongoDb4";

    /**
     * Builds new {@link MongoDb4Provider} instance.
     *
     * @param <B> the builder type.
     */
    public static class Builder<B extends Builder<B>> extends AbstractFilterable.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<MongoDb4Provider> {

        @PluginBuilderAttribute(value = "connection")
        @Required(message = "No connection string provided")
        private String connectionStringSource;

        @PluginBuilderAttribute
        private long collectionSize = DEFAULT_COLLECTION_SIZE;

        @PluginBuilderAttribute("capped")
        private boolean capped = false;

        @PluginBuilderAttribute("collectionName")
        private String collectionName;

        @PluginBuilderAttribute("databaseName")
        private String databaseName;

        @Override
        public MongoDb4Provider build() {
            LOGGER.warn("The {} Appender is deprecated, use the MongoDb Appender instead.", PLUGIN_NAME);
            return newMongoDb4Provider();
        }

        protected MongoDb4Provider newMongoDb4Provider() {
            return new MongoDb4Provider(connectionStringSource, databaseName, collectionName, capped, collectionSize);
        }

        /**
         * Sets the MongoDB connection string.
         *
         * @param connectionStringSource the MongoDB connection string.
         * @return this instance.
         */
        public B setConnectionStringSource(final String connectionStringSource) {
            this.connectionStringSource = connectionStringSource;
            return asBuilder();
        }

        /**
         * Sets whether the MongoDB collection is capped.
         *
         * @param isCapped whether the MongoDB collection is capped.
         * @return this instance.
         */
        public B setCapped(final boolean isCapped) {
            this.capped = isCapped;
            return asBuilder();
        }

        /**
         * Sets the maximum size in bytes of a capped collection.
         *
         * @param sizeInBytes the maximum size in bytes of a capped collection.
         * @return this instance.
         */
        public B setCollectionSize(final int sizeInBytes) {
            this.collectionSize = sizeInBytes;
            return asBuilder();
        }

        /**
         * Sets the maximum size in bytes of a capped collection.
         *
         * @param sizeInBytes the maximum size in bytes of a capped collection.
         * @return this instance.
         */
        public B setCollectionSize(final long sizeInBytes) {
            this.collectionSize = sizeInBytes;
            return asBuilder();
        }

        /**
         * Sets name of the collection for the appender to output to
         *
         * @param collectionName the name of the collection for the appender to output to
         * @return this instance.
         */
        public B setCollectionName(final String collectionName) {
            this.collectionName = collectionName;
            return asBuilder();
        }

        /**
         * Sets the name of the logical database for the appender to output to.
         *
         * @param databaseName the name of the DB for the appender to output to
         * @return this instance.
         */
        public B setDatabaseName(final String databaseName) {
            this.databaseName = databaseName;
            return asBuilder();
        }
    }

    private static final CodecRegistry CODEC_REGISTRIES = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(MongoDb4LevelCodec.INSTANCE),
            CodecRegistries.fromCodecs(new MongoDb4DocumentObjectCodec()));

    // TODO Where does this number come from?
    private static final long DEFAULT_COLLECTION_SIZE = 536_870_912;

    /**
     * Creates a new builder.
     *
     * @param <B> The builder type.
     * @return a new builder.
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final Long collectionSize;
    private final boolean isCapped;
    private final String collectionName;
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final ConnectionString connectionString;

    private MongoDb4Provider(
            final String connectionStringSource,
            final String databaseName,
            final String collectionName,
            final boolean isCapped,
            final Long collectionSize) {
        this.connectionString = createConnectionString(connectionStringSource);
        final MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(this.connectionString)
                .codecRegistry(CODEC_REGISTRIES)
                .build();
        this.mongoClient = MongoClients.create(settings);
        this.mongoDatabase = createDatabase(connectionString, databaseName, mongoClient);
        this.isCapped = isCapped;
        this.collectionSize = collectionSize;
        this.collectionName = getEffectiveCollectionName(connectionString, collectionName);
        LOGGER.debug("instantiated {}", this);
    }

    private static ConnectionString createConnectionString(final String connectionStringSource) {
        try {
            return new ConnectionString(connectionStringSource);
        } catch (final IllegalArgumentException error) {
            final String message = String.format("Invalid MongoDB connection string: `%s`", connectionStringSource);
            throw new IllegalArgumentException(message, error);
        }
    }

    private static MongoDatabase createDatabase(
            final ConnectionString connectionString, final String databaseName, final MongoClient client) {
        final String effectiveDatabaseName = databaseName != null ? databaseName : connectionString.getDatabase();
        try {
            // noinspection DataFlowIssue
            MongoNamespace.checkDatabaseNameValidity(effectiveDatabaseName);
        } catch (final IllegalArgumentException error) {
            final String message = String.format("Invalid MongoDB database name: `%s`", effectiveDatabaseName);
            throw new IllegalArgumentException(message, error);
        }
        return client.getDatabase(effectiveDatabaseName);
    }

    private static String getEffectiveCollectionName(
            final ConnectionString connectionString, final String collectionName) {
        final String effectiveCollectionName =
                collectionName != null ? collectionName : connectionString.getCollection();
        try {
            // noinspection DataFlowIssue
            MongoNamespace.checkCollectionNameValidity(effectiveCollectionName);
        } catch (final IllegalArgumentException error) {
            final String message = String.format("Invalid MongoDB collection name: `%s`", effectiveCollectionName);
            throw new IllegalArgumentException(message, error);
        }
        return effectiveCollectionName;
    }

    @Override
    public MongoDb4Connection getConnection() {
        return new MongoDb4Connection(
                connectionString, mongoClient, mongoDatabase, collectionName, isCapped, collectionSize);
    }

    @Override
    public String toString() {
        return String.format(
                "%s [connectionString=`%s`, collectionSize=%s, isCapped=%s, databaseName=`%s`, collectionName=`%s`]",
                MongoDb4Provider.class.getSimpleName(),
                connectionString,
                collectionSize,
                isCapped,
                mongoDatabase.getName(),
                collectionName);
    }
}
