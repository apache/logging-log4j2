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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import java.lang.reflect.Field;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class MongoDb4ProviderTest {

    private static final String CON_STR_WO_DB = "mongodb://localhost:27017";

    private static final String CON_STR_W_DB = "mongodb://localhost:27017/logging";

    private static final String CON_STR_DB_COLL = "mongodb://localhost:27017/logging.logs";

    private static final String COLLECTION_NAME = "logsTest";

    private static final String DATABASE_NAME = "loggingTest";

    @Test
    void createProviderWithDatabaseAndCollectionProvidedViaConfig() {
        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_WO_DB)
                .setDatabaseName(DATABASE_NAME)
                .setCollectionName(COLLECTION_NAME)
                .build();
        assertThat(provider).isNotNull();
        assertProviderNamespace(provider, DATABASE_NAME, COLLECTION_NAME);
    }

    @Test
    void createProviderWithoutDatabaseName() {
        assertThatThrownBy(() -> MongoDb4Provider.newBuilder()
                        .setConnectionStringSource(CON_STR_WO_DB)
                        .build())
                .hasMessage("Invalid MongoDB database name: `null`");
    }

    @Test
    void createProviderWithoutDatabaseNameWithCollectionName() {
        assertThatThrownBy(() -> MongoDb4Provider.newBuilder()
                        .setConnectionStringSource(CON_STR_WO_DB)
                        .setCollectionName(COLLECTION_NAME)
                        .build())
                .hasMessage("Invalid MongoDB database name: `null`");
    }

    @Test
    void createProviderWithoutCollectionName() {
        assertThatThrownBy(() -> MongoDb4Provider.newBuilder()
                        .setConnectionStringSource(CON_STR_WO_DB)
                        .setDatabaseName(DATABASE_NAME)
                        .build())
                .hasMessage("Invalid MongoDB collection name: `null`");
    }

    @Test
    void createProviderWithDatabaseOnConnectionString() {
        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_W_DB)
                .setCollectionName(COLLECTION_NAME)
                .build();
        assertThat(provider).isNotNull();
        assertProviderNamespace(provider, "logging", COLLECTION_NAME);
    }

    @Test
    void createProviderConfigOverridesConnectionString() {
        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_DB_COLL)
                .setCollectionName(COLLECTION_NAME)
                .setDatabaseName(DATABASE_NAME)
                .build();
        assertThat(provider).isNotNull();
        assertProviderNamespace(provider, DATABASE_NAME, COLLECTION_NAME);
    }

    private static void assertProviderNamespace(MongoDb4Provider provider, String databaseName, String collectionName) {
        MongoNamespace namespace = providerNamespace(provider);
        assertThat(namespace.getDatabaseName()).isEqualTo(databaseName);
        assertThat(namespace.getCollectionName()).isEqualTo(collectionName);
    }

    private static MongoNamespace providerNamespace(MongoDb4Provider provider) {
        try {
            MongoDb4Connection connection = provider.getConnection();
            Field collectionField = MongoDb4Connection.class.getDeclaredField("collection");
            collectionField.setAccessible(true);
            @SuppressWarnings("unchecked")
            MongoCollection<Document> collection = (MongoCollection<Document>) collectionField.get(connection);
            return collection.getNamespace();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
