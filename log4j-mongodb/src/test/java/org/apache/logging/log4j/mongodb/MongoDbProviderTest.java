package org.apache.logging.log4j.mongodb;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


class MongoDbProviderTest {

    private String validConnectionStringWithoutDatabase = "mongodb://localhost:27017";
    private String invalidConnectionString = "test:test";
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

        assertNotNull("Returned provider is null", provider);
        assertEquals("Collection names do not match", this.collectionName, provider.getConnection().getCollection().getNamespace().getCollectionName());
        assertEquals("Database names do not match", this.databaseName, provider.getConnection().getCollection().getNamespace().getDatabaseName());

    }

    @Test
    void createProviderWithoutDatabaseName() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .build();

        assertNull("Provider should be null but was not", provider);


    }

    @Test
    void createProviderWithoutDatabaseNameWithCollectionName(){

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .setCollectionName(this.collectionName)
                .build();

        assertNull("Provider should be null but was not", provider);



    }

    @Test
    void createProviderWithoutCollectionName(){

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .setDatabaseName(this.databaseName)
                .build();

        assertNull("Provider should be null but was not", provider);


    }

    @Test
    void createProviderWithDatabaseOnConnectionString(){
        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithDatabase)
                .setCollectionName(this.collectionName)
                .build();

        assertNotNull("Provider should be null but was not", provider);
        assertEquals("Collection names do not match", this.collectionName, provider.getConnection().getCollection().getNamespace().getCollectionName());
        assertEquals("Database names do not match", "logging", provider.getConnection().getCollection().getNamespace().getDatabaseName());

    }

    @Test
    void createProviderConfigOverridesConnectionString() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithDatabaseAndCollection)
                .setCollectionName(this.collectionName)
                .setDatabaseName(this.databaseName)
                .build();

        assertNotNull("Provider should not be null", provider);
        assertEquals("Collection name does not match provided configuration", this.collectionName, provider.getConnection().getCollection().getNamespace().getCollectionName());
        assertEquals("Database name does not match provided configuration", this.databaseName, provider.getConnection().getCollection().getNamespace().getDatabaseName());

    }




}