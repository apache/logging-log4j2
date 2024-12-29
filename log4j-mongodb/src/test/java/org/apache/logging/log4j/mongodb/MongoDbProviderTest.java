package org.apache.logging.log4j.mongodb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


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
        assertEquals(this.collectionName, provider.getConnection().getCollection().getNamespace().getCollectionName(), "Collection names do not match");
        assertEquals( this.databaseName, provider.getConnection().getCollection().getNamespace().getDatabaseName(), "Database names do not match");

    }

    @Test
    void createProviderWithoutDatabaseName() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .build();

        assertNull( provider, "Provider should be null but was not");


    }

    @Test
    void createProviderWithoutDatabaseNameWithCollectionName(){

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .setCollectionName(this.collectionName)
                .build();

        assertNull(provider,"Provider should be null but was not");



    }

    @Test
    void createProviderWithoutCollectionName(){

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithoutDatabase)
                .setDatabaseName(this.databaseName)
                .build();

        assertNull(provider,"Provider should be null but was not");


    }

    @Test
    void createProviderWithDatabaseOnConnectionString(){
        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithDatabase)
                .setCollectionName(this.collectionName)
                .build();

        assertNotNull(provider,"Provider should not be null");
        assertEquals(this.collectionName, provider.getConnection().getCollection().getNamespace().getCollectionName(), "Provider should be null but was not");
        assertEquals( "logging", provider.getConnection().getCollection().getNamespace().getDatabaseName(),"Database names do not match");

    }

    @Test
    void createProviderConfigOverridesConnectionString() {

        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionStringWithDatabaseAndCollection)
                .setCollectionName(this.collectionName)
                .setDatabaseName(this.databaseName)
                .build();

        assertNotNull(provider,"Provider should not be null");
        assertEquals(this.collectionName, provider.getConnection().getCollection().getNamespace().getCollectionName(), "Collection name does not match provided configuration");
        assertEquals(this.databaseName, provider.getConnection().getCollection().getNamespace().getDatabaseName(), "Database name does not match provided configuration");

    }




}