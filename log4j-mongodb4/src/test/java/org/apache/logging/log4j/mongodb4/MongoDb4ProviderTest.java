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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class MongoDb4ProviderTest {

    private static final String CON_STR_WO_DB = "mongodb://localhost:27017";
    private static final String CON_STR_W_DB = "mongodb://localhost:27017/logging";
    private static final String CON_STR_DB_COLL = "mongodb://localhost:27017/logging.logs";

    private static final String collectionName = "logsTest";
    private static final String databaseName = "loggingTest";

    @Test
    void createProviderWithDatabaseAndCollectionProvidedViaConfig() {

        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_WO_DB)
                .setDatabaseName(databaseName)
                .setCollectionName(collectionName)
                .build();

        assertNotNull(provider);
        assertEquals(
                collectionName,
                provider.getConnection().getCollection().getNamespace().getCollectionName());
        assertEquals(
                databaseName,
                provider.getConnection().getCollection().getNamespace().getDatabaseName());
    }

    @Test
    void createProviderWithoutDatabaseName() {

        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_WO_DB)
                .build();

        assertNull(provider);
    }

    @Test
    void createProviderWithoutDatabaseNameWithCollectionName() {

        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_WO_DB)
                .setCollectionName(collectionName)
                .build();

        assertNull(provider);
    }

    @Test
    void createProviderWithoutCollectionName() {

        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_WO_DB)
                .setDatabaseName(databaseName)
                .build();

        assertNull(provider);
    }

    @Test
    void createProviderWithDatabaseOnConnectionString() {
        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_W_DB)
                .setCollectionName(collectionName)
                .build();

        assertNotNull(provider);

        MongoNamespace namespace = findProviderNamespace(provider);
        assertEquals(collectionName, namespace.getCollectionName());

        assertEquals("logging", namespace.getDatabaseName());
    }

    @Test
    void createProviderConfigOverridesConnectionString() {

        MongoDb4Provider provider = MongoDb4Provider.newBuilder()
                .setConnectionStringSource(CON_STR_DB_COLL)
                .setCollectionName(collectionName)
                .setDatabaseName(databaseName)
                .build();

        assertNotNull(provider);
        MongoNamespace namespace = findProviderNamespace(provider);
        assertEquals(collectionName, namespace.getCollectionName());
        assertEquals(databaseName, namespace.getDatabaseName());
    }

    private static MongoNamespace findProviderNamespace(final MongoDb4Provider provider) {
        MongoDb4Connection connection = provider.getConnection();
        try {
            Field collectionField = connection.getClass().getDeclaredField("collection");
            collectionField.setAccessible(true);
            MongoCollection<?> collection = (MongoCollection<?>) collectionField.get(connection);
            return collection.getNamespace();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
