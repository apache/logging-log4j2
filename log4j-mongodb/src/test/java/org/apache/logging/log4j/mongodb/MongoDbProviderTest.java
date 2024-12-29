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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MongoDbProviderTest {

    private String validConnectionStringWithoutDatabase = "mongodb://localhost:27017";
    private String validConnectionStringWithDatabase = "mongodb://localhost:27017/logging";
    private String validConnectionStringWithDatabaseAndCollection = "mongodb://localhost:27017/logging.logs";

    private String collectionName = "logsTest";
    private String databaseName = "loggingTest";

    @Test
    void createProviderWithDatabaseAndCollectionProvidedViaConfig() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .setDatabaseName(this.databaseName)
                .setCollectionName(this.collectionName)
                .build();

        assertNotNull(provider, "Returned provider is null");
        assertEquals(
                this.collectionName,
                provider.getConnection().getCollection().getNamespace().getCollectionName(),
                "Collection names do not match");
        assertEquals(
                this.databaseName,
                provider.getConnection().getCollection().getNamespace().getDatabaseName(),
                "Database names do not match");
    }

    @Test
    void createProviderWithoutDatabaseName() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .build();

        assertNull(provider, "Provider should be null but was not");
    }

    @Test
    void createProviderWithoutDatabaseNameWithCollectionName() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .setCollectionName(this.collectionName)
                .build();

        assertNull(provider, "Provider should be null but was not");
    }

    @Test
    void createProviderWithoutCollectionName() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .setDatabaseName(this.databaseName)
                .build();

        assertNull(provider, "Provider should be null but was not");
    }

    @Test
    void createProviderWithDatabaseOnConnectionString() {
        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithDatabase)
                .setCollectionName(this.collectionName)
                .build();

        assertNotNull(provider, "Provider should not be null");
        assertEquals(
                this.collectionName,
                provider.getConnection().getCollection().getNamespace().getCollectionName(),
                "Provider should be null but was not");
        assertEquals(
                "logging",
                provider.getConnection().getCollection().getNamespace().getDatabaseName(),
                "Database names do not match");
    }

    @Test
    void createProviderConfigOverridesConnectionString() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithDatabaseAndCollection)
                .setCollectionName(this.collectionName)
                .setDatabaseName(this.databaseName)
                .build();

        assertNotNull(provider, "Provider should not be null");
        assertEquals(
                this.collectionName,
                provider.getConnection().getCollection().getNamespace().getCollectionName(),
                "Collection name does not match provided configuration");
        assertEquals(
                this.databaseName,
                provider.getConnection().getCollection().getNamespace().getDatabaseName(),
                "Database name does not match provided configuration");
    }
}
