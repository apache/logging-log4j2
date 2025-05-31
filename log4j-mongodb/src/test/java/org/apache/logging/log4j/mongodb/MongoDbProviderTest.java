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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import java.lang.reflect.Field;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class MongoDbProviderTest {

    private static final String CS_WO_DB = "mongodb://localhost:27017";
    private static final String CS_W_DB = "mongodb://localhost:27017/logging";
    private static final String CS_W_DB_N_COLL = "mongodb://localhost:27017/logging.logs";

    private static final String COLL_NAME = "logsTest";
    private static final String DB_NAME = "loggingTest";

    @Test
    void createProviderWithDatabaseAndCollectionProvidedViaConfig() {
        final MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(CS_WO_DB)
                .setDatabaseName(DB_NAME)
                .setCollectionName(COLL_NAME)
                .build();
        final MongoNamespace namespace = getNamespace(provider.getConnection());
        assertThat(namespace.getCollectionName()).isEqualTo(COLL_NAME);
        assertThat(namespace.getDatabaseName()).isEqualTo(DB_NAME);
    }

    @Test
    void createProviderWithoutDatabaseName() {
        assertThatThrownBy(() -> MongoDbProvider.newBuilder()
                        .setConnectionStringSource(CS_WO_DB)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid MongoDB database name");
    }

    @Test
    void createProviderWithoutDatabaseNameWithCollectionName() {
        assertThatThrownBy(() -> MongoDbProvider.newBuilder()
                        .setConnectionStringSource(CS_WO_DB)
                        .setCollectionName(COLL_NAME)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid MongoDB database name");
    }

    @Test
    void createProviderWithoutCollectionName() {
        assertThatThrownBy(() -> MongoDbProvider.newBuilder()
                        .setConnectionStringSource(CS_WO_DB)
                        .setDatabaseName(DB_NAME)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid MongoDB collection name");
    }

    @Test
    void createProviderWithDatabaseOnConnectionString() {
        final MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(CS_W_DB)
                .setCollectionName(COLL_NAME)
                .build();
        final MongoNamespace namespace = getNamespace(provider.getConnection());
        assertThat(namespace.getCollectionName()).isEqualTo(COLL_NAME);
        assertThat(namespace.getDatabaseName()).isEqualTo("logging");
    }

    @Test
    void createProviderConfigOverridesConnectionString() {
        final MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(CS_W_DB_N_COLL)
                .setCollectionName(COLL_NAME)
                .setDatabaseName(DB_NAME)
                .build();
        final MongoNamespace namespace = getNamespace(provider.getConnection());
        assertThat(namespace.getCollectionName()).isEqualTo(COLL_NAME);
        assertThat(namespace.getDatabaseName()).isEqualTo(DB_NAME);
    }

    private static MongoNamespace getNamespace(final MongoDbConnection connection) {
        try {
            final Field collectionField = MongoDbConnection.class.getDeclaredField("collection");
            collectionField.setAccessible(true);
            @SuppressWarnings("unchecked")
            final MongoCollection<Document> collection = (MongoCollection<Document>) collectionField.get(connection);
            return collection.getNamespace();
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
