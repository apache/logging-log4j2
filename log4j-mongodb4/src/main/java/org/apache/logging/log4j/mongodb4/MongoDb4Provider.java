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
package org.apache.logging.log4j.mongodb4;

import org.apache.logging.log4j.Logger;
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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * The MongoDB implementation of {@link NoSqlProvider} using the MongoDB driver
 * version 4 API.
 */
@Plugin(name = "MongoDb4", category = Core.CATEGORY_NAME, printObject = true)
public final class MongoDb4Provider implements NoSqlProvider<MongoDb4Connection> {

    public static class Builder<B extends Builder<B>> extends AbstractFilterable.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<MongoDb4Provider> {

        @PluginBuilderAttribute(value = "connection")
        @Required(message = "No connection string provided")
        private String connectionStringSource;

        @PluginBuilderAttribute
        private int collectionSize = DEFAULT_COLLECTION_SIZE;

        @PluginBuilderAttribute("capped")
        private boolean capped = false;

        @Override
        public MongoDb4Provider build() {
            return new MongoDb4Provider(connectionStringSource, capped, collectionSize);
        }

        public B setCapped(final boolean isCapped) {
            this.capped = isCapped;
            return asBuilder();
        }

        public B setCollectionSize(final int collectionSize) {
            this.collectionSize = collectionSize;
            return asBuilder();
        }
    }

    private static final Logger LOGGER = StatusLogger.getLogger();

    // @formatter:off
    private static final CodecRegistry CODEC_REGISTRIES = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(MongoDb4LevelCodec.INSTANCE));
    // @formatter:on

    // TODO Where does this number come from?
    private static final int DEFAULT_COLLECTION_SIZE = 536_870_912;

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final Integer collectionSize;
    private final boolean isCapped;
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final ConnectionString connectionString;

    private MongoDb4Provider(final String connectionStringSource, final boolean isCapped,
            final Integer collectionSize) {
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
        String databaseName = this.connectionString.getDatabase();
        LOGGER.debug("Getting MongoDatabase {}...", databaseName);
        this.mongoDatabase = this.mongoClient.getDatabase(databaseName);
        LOGGER.debug("Got MongoDatabase {}", mongoDatabase);
        this.isCapped = isCapped;
        this.collectionSize = collectionSize;
    }

    @Override
    public MongoDb4Connection getConnection() {
        return new MongoDb4Connection(connectionString, mongoClient, mongoDatabase, isCapped, collectionSize);
    }

    @Override
    public String toString() {
        return String.format(
                "%s [connectionString=%s, collectionSize=%s, isCapped=%s, mongoClient=%s, mongoDatabase=%s]",
                MongoDb4Provider.class.getSimpleName(), connectionString, collectionSize, isCapped, mongoClient,
                mongoDatabase);
    }

}
