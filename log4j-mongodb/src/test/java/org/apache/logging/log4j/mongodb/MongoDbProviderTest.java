package org.apache.logging.log4j.mongodb;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.plugins.util.Assert;
import org.bson.Document;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class MongoDbProviderTest {

    private String validConnectionString = "mongodb://localhost:27017";
    private String invalidConnectionString = "test:test";

    @Mock
    private MongoDatabase mockDB;

    @Mock
    private MongoCollection<Document> mockCollection;

    @Mock
    private MongoClient mockClient;

    @Mock
    private MongoNamespace mockNamespace;

    @Before
    public void initMocks(){
        when(mockClient.getDatabase(anyString())).thenReturn(mockDB);

    }



    @Test
    void databaseNameAndCollectionNameProvided() {
        String collectionName = "logsTest";
        String databaseName = "loggingTest";

        MongoCollection mockCollection = mock(MongoCollection.class);
        MongoNamespace mockNamespace = mock(MongoNamespace.class);
        when(mockNamespace.getCollectionName()).thenReturn(collectionName);
        when(mockNamespace.getDatabaseName()).thenReturn(databaseName);
        when(mockCollection.getNamespace()).thenReturn(mockNamespace);


        MongoDbProvider provider = MongoDbProvider.newBuilder()
                .setConnectionStringSource(this.validConnectionString)
                .setDatabaseName(databaseName)
                .setCollectionName(collectionName)
                .build();

        assertNotNull("Returned provider is null", provider);
        assertEquals("Collection names do not match", collectionName, provider.getConnection().getCollection().getNamespace().getCollectionName());
        assertEquals("Database names do not match", databaseName, provider.getConnection().getCollection().getNamespace().getDatabaseName());

    }

    @Test
    void databaseNameOmitted() {

    }

    @Test
    void collectionNameProvided(){


    }

    @Test
    void collectionNameOmitted(){

    }

    @Test
    void connectionStringValid(){

    }

    @Test
    void connectionStringEmpty() {

    }

    @Test
    void connectionStringInvalid(){

    }


}