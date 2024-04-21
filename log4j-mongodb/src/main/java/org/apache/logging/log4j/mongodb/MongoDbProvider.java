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
package org.apache.logging.log4j.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.nosql.NoSqlProvider;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * The MongoDB implementation of {@link NoSqlProvider} using the MongoDB driver
 * version 5 API.
 */
@Configurable(printObject = true)
@Plugin("MongoDb")
public final class MongoDbProvider implements NoSqlProvider<MongoDbConnection> {

    public static class Builder<B extends Builder<B>> extends AbstractFilterable.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<MongoDbProvider> {

        @PluginAttribute(value = "connection")
        @Required(message = "No connection string provided")
        private String connectionStringSource;

        @PluginAttribute
        private long collectionSize = DEFAULT_COLLECTION_SIZE;

        @PluginAttribute("capped")
        private boolean capped = false;

        @Override
        public MongoDbProvider build() {
            return new MongoDbProvider(connectionStringSource, capped, collectionSize);
        }

        public B setConnectionStringSource(final String connectionStringSource) {
            this.connectionStringSource = connectionStringSource;
            return asBuilder();
        }

        public B setCapped(final boolean isCapped) {
            this.capped = isCapped;
            return asBuilder();
        }

        public B setCollectionSize(final long collectionSize) {
            this.collectionSize = collectionSize;
            return asBuilder();
        }
    }

    private static final Logger LOGGER = StatusLogger.getLogger();

    // @formatter:off
    private static final CodecRegistry CODEC_REGISTRIES = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(MongoDbLevelCodec.INSTANCE),
            CodecRegistries.fromCodecs(new MongoDbDocumentObjectCodec()));
    // @formatter:on

    // TODO Where does this number come from?
    private static final long DEFAULT_COLLECTION_SIZE = 536_870_912;

    @PluginFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final Long collectionSize;
    private final boolean isCapped;
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final ConnectionString connectionString;

    private MongoDbProvider(final String connectionStringSource, final boolean isCapped, final Long collectionSize) {
        LOGGER.debug("Creating ConnectionString {}...", connectionStringSource);
        this.connectionString = new ConnectionString(connectionStringSource);
        LOGGER.debug("Created ConnectionString {}", connectionString);
        LOGGER.debug("Creating MongoClientSettings...");
        // @formatter:off
        final MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(this.connectionString)
                .codecRegistry(CODEC_REGISTRIES)
                .build();
        // @formatter:on
        LOGGER.debug("Created MongoClientSettings {}", settings);
        LOGGER.debug("Creating MongoClient {}...", settings);
        this.mongoClient = MongoClients.create(settings);
        LOGGER.debug("Created MongoClient {}", mongoClient);
        final String databaseName = this.connectionString.getDatabase();
        LOGGER.debug("Getting MongoDatabase {}...", databaseName);
        this.mongoDatabase = this.mongoClient.getDatabase(databaseName);
        LOGGER.debug("Got MongoDatabase {}", mongoDatabase);
        this.isCapped = isCapped;
        this.collectionSize = collectionSize;
    }

    @Override
    public MongoDbConnection getConnection() {
        return new MongoDbConnection(connectionString, mongoClient, mongoDatabase, isCapped, collectionSize);
    }

    @Override
    public String toString() {
        return String.format(
                "%s [connectionString=%s, collectionSize=%s, isCapped=%s, mongoClient=%s, mongoDatabase=%s]",
                MongoDbProvider.class.getSimpleName(),
                connectionString,
                collectionSize,
                isCapped,
                mongoClient,
                mongoDatabase);
    }
}
