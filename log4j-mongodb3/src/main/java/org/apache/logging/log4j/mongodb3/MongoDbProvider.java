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
package org.apache.logging.log4j.mongodb3;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.nosql.NoSqlProvider;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.Strings;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * The MongoDB implementation of {@link NoSqlProvider}.using the MongoDB driver version 3 API.
 */
@Plugin(name = "MongoDb3", category = Core.CATEGORY_NAME, printObject = true)
public final class MongoDbProvider implements NoSqlProvider<MongoDbConnection> {

    public static class Builder<B extends Builder<B>> extends AbstractFilterable.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<MongoDbProvider> {

        // @formatter:off
        private static final CodecRegistry CODEC_REGISTRIES = CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(LevelCodec.INSTANCE),
                CodecRegistries.fromCodecs(new MongoDbDocumentObjectCodec()));
        // @formatter:on

        private static WriteConcern toWriteConcern(
                final String writeConcernConstant, final String writeConcernConstantClassName) {
            WriteConcern writeConcern;
            if (Strings.isNotEmpty(writeConcernConstant)) {
                if (Strings.isNotEmpty(writeConcernConstantClassName)) {
                    try {
                        final Class<?> writeConcernConstantClass = LoaderUtil.loadClass(writeConcernConstantClassName);
                        final Field field = writeConcernConstantClass.getField(writeConcernConstant);
                        writeConcern = (WriteConcern) field.get(null);
                    } catch (final Exception e) {
                        LOGGER.error(
                                "Write concern constant [{}.{}] not found, using default.",
                                writeConcernConstantClassName,
                                writeConcernConstant);
                        writeConcern = DEFAULT_WRITE_CONCERN;
                    }
                } else {
                    writeConcern = WriteConcern.valueOf(writeConcernConstant);
                    if (writeConcern == null) {
                        LOGGER.warn("Write concern constant [{}] not found, using default.", writeConcernConstant);
                        writeConcern = DEFAULT_WRITE_CONCERN;
                    }
                }
            } else {
                writeConcern = DEFAULT_WRITE_CONCERN;
            }
            return writeConcern;
        }

        @PluginBuilderAttribute
        @Required(message = "No collection name provided")
        private String collectionName;

        @PluginBuilderAttribute
        private int collectionSize = DEFAULT_COLLECTION_SIZE;

        @PluginBuilderAttribute
        @Required(message = "No database name provided")
        private String databaseName;

        @PluginBuilderAttribute
        private String factoryClassName;

        @PluginBuilderAttribute
        private String factoryMethodName;

        @PluginBuilderAttribute("capped")
        private boolean capped = false;

        @PluginBuilderAttribute(sensitive = true)
        private String password;

        @PluginBuilderAttribute
        @ValidPort
        private String port = "" + DEFAULT_PORT;

        @PluginBuilderAttribute
        @ValidHost
        private String server = "localhost";

        @PluginBuilderAttribute
        private String userName;

        @PluginBuilderAttribute
        private String writeConcernConstant;

        @PluginBuilderAttribute
        private String writeConcernConstantClassName;

        @SuppressWarnings("resource")
        @Override
        public MongoDbProvider build() {
            MongoDatabase database;
            String description;
            MongoClient mongoClient = null;

            if (Strings.isNotEmpty(factoryClassName) && Strings.isNotEmpty(factoryMethodName)) {
                try {
                    final Class<?> factoryClass = LoaderUtil.loadClass(factoryClassName);
                    final Method method = factoryClass.getMethod(factoryMethodName);
                    final Object object = method.invoke(null);

                    if (object instanceof MongoDatabase) {
                        database = (MongoDatabase) object;
                    } else if (object instanceof MongoClient) {
                        if (Strings.isNotEmpty(databaseName)) {
                            database = ((MongoClient) object).getDatabase(databaseName);
                        } else {
                            LOGGER.error(
                                    "The factory method [{}.{}()] returned a MongoClient so the database name is "
                                            + "required.",
                                    factoryClassName,
                                    factoryMethodName);
                            return null;
                        }
                    } else {
                        if (object == null) {
                            LOGGER.error(
                                    "The factory method [{}.{}()] returned null.", factoryClassName, factoryMethodName);
                        } else {
                            LOGGER.error(
                                    "The factory method [{}.{}()] returned an unsupported type [{}].",
                                    factoryClassName,
                                    factoryMethodName,
                                    object.getClass().getName());
                        }
                        return null;
                    }

                    final String databaseName = database.getName();
                    description = "database=" + databaseName;
                } catch (final ClassNotFoundException e) {
                    LOGGER.error("The factory class [{}] could not be loaded.", factoryClassName, e);
                    return null;
                } catch (final NoSuchMethodException e) {
                    LOGGER.error(
                            "The factory class [{}] does not have a no-arg method named [{}].",
                            factoryClassName,
                            factoryMethodName,
                            e);
                    return null;
                } catch (final Exception e) {
                    LOGGER.error(
                            "The factory method [{}.{}()] could not be invoked.",
                            factoryClassName,
                            factoryMethodName,
                            e);
                    return null;
                }
            } else if (Strings.isNotEmpty(databaseName)) {
                MongoCredential mongoCredential = null;
                description = "database=" + databaseName;
                if (Strings.isNotEmpty(userName) && Strings.isNotEmpty(password)) {
                    description += ", username=" + userName;
                    mongoCredential = MongoCredential.createCredential(userName, databaseName, password.toCharArray());
                }
                try {
                    final int portInt = TypeConverters.convert(port, int.class, DEFAULT_PORT);
                    description += ", server=" + server + ", port=" + portInt;
                    final WriteConcern writeConcern =
                            toWriteConcern(writeConcernConstant, writeConcernConstantClassName);
                    // @formatter:off
                    final MongoClientOptions options = MongoClientOptions.builder()
                            .codecRegistry(CODEC_REGISTRIES)
                            .writeConcern(writeConcern)
                            .build();
                    // @formatter:on
                    final ServerAddress serverAddress = new ServerAddress(server, portInt);
                    mongoClient = mongoCredential == null
                            ?
                            // @formatter:off
                            new MongoClient(serverAddress, options)
                            : new MongoClient(serverAddress, mongoCredential, options);
                    // @formatter:on
                    database = mongoClient.getDatabase(databaseName);
                } catch (final Exception e) {
                    LOGGER.error(
                            "Failed to obtain a database instance from the MongoClient at server [{}] and "
                                    + "port [{}].",
                            server,
                            port);
                    close(mongoClient);
                    return null;
                }
            } else {
                LOGGER.error("No factory method was provided so the database name is required.");
                close(mongoClient);
                return null;
            }

            try {
                database.listCollectionNames().first(); // Check if the database actually requires authentication
            } catch (final Exception e) {
                LOGGER.error(
                        "The database is not up, or you are not authenticated, try supplying a username and password to the MongoDB provider.",
                        e);
                close(mongoClient);
                return null;
            }

            return new MongoDbProvider(mongoClient, database, collectionName, capped, collectionSize, description);
        }

        private void close(final MongoClient mongoClient) {
            if (mongoClient != null) {
                mongoClient.close();
            }
        }

        public B setCapped(final boolean isCapped) {
            this.capped = isCapped;
            return asBuilder();
        }

        public B setCollectionName(final String collectionName) {
            this.collectionName = collectionName;
            return asBuilder();
        }

        public B setCollectionSize(final int collectionSize) {
            this.collectionSize = collectionSize;
            return asBuilder();
        }

        public B setDatabaseName(final String databaseName) {
            this.databaseName = databaseName;
            return asBuilder();
        }

        public B setFactoryClassName(final String factoryClassName) {
            this.factoryClassName = factoryClassName;
            return asBuilder();
        }

        public B setFactoryMethodName(final String factoryMethodName) {
            this.factoryMethodName = factoryMethodName;
            return asBuilder();
        }

        public B setPassword(final String password) {
            this.password = password;
            return asBuilder();
        }

        public B setPort(final String port) {
            this.port = port;
            return asBuilder();
        }

        public B setServer(final String server) {
            this.server = server;
            return asBuilder();
        }

        public B setUserName(final String userName) {
            this.userName = userName;
            return asBuilder();
        }

        public B setWriteConcernConstant(final String writeConcernConstant) {
            this.writeConcernConstant = writeConcernConstant;
            return asBuilder();
        }

        public B setWriteConcernConstantClassName(final String writeConcernConstantClassName) {
            this.writeConcernConstantClassName = writeConcernConstantClassName;
            return asBuilder();
        }
    }

    private static final int DEFAULT_COLLECTION_SIZE = 536870912;
    private static final int DEFAULT_PORT = 27017;
    private static final WriteConcern DEFAULT_WRITE_CONCERN = WriteConcern.ACKNOWLEDGED;

    private static final Logger LOGGER = StatusLogger.getLogger();

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String collectionName;
    private final Integer collectionSize;
    private final String description;
    private final boolean isCapped;
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;

    private MongoDbProvider(
            final MongoClient mongoClient,
            final MongoDatabase mongoDatabase,
            final String collectionName,
            final boolean isCapped,
            final Integer collectionSize,
            final String description) {
        this.mongoClient = mongoClient;
        this.mongoDatabase = mongoDatabase;
        this.collectionName = collectionName;
        this.isCapped = isCapped;
        this.collectionSize = collectionSize;
        this.description = "mongoDb{ " + description + " }";
    }

    @Override
    public MongoDbConnection getConnection() {
        return new MongoDbConnection(mongoClient, mongoDatabase, collectionName, isCapped, collectionSize);
    }

    @Override
    public String toString() {
        return description;
    }
}
