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
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.nosql.AbstractNoSqlConnection;
import org.apache.logging.log4j.core.appender.nosql.NoSqlConnection;
import org.apache.logging.log4j.core.appender.nosql.NoSqlObject;
import org.apache.logging.log4j.status.StatusLogger;
import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.result.InsertOneResult;

/**
 * The MongoDB implementation of {@link NoSqlConnection}.
 */
public final class MongoDb4Connection extends AbstractNoSqlConnection<Document, MongoDb4DocumentObject> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static MongoCollection<Document> getOrCreateMongoCollection(final MongoDatabase database,
            final String collectionName, final boolean isCapped, final Integer sizeInBytes) {
        try {
            LOGGER.debug("Gettting collection '{}'...", collectionName);
            // throws IllegalArgumentException if collectionName is invalid
            final MongoCollection<Document> found = database.getCollection(collectionName);
            LOGGER.debug("Got collection {}", found);
            return found;
        } catch (final IllegalStateException e) {
            LOGGER.debug("Collection '{}' does not exist.", collectionName);
            final CreateCollectionOptions options = new CreateCollectionOptions().capped(isCapped)
                    .sizeInBytes(sizeInBytes);
            LOGGER.debug("Creating collection '{}' with options {}...", collectionName, options);
            database.createCollection(collectionName, options);
            LOGGER.debug("Created collection.");
            final MongoCollection<Document> created = database.getCollection(collectionName);
            LOGGER.debug("Got created collection {}", created);
            return created;
        }

    }

    private final ConnectionString connectionString;
    private final MongoCollection<Document> collection;
    private final MongoClient mongoClient;

    public MongoDb4Connection(final ConnectionString connectionString, final MongoClient mongoClient,
            final MongoDatabase mongoDatabase, final boolean isCapped, final Integer sizeInBytes) {
        this.connectionString = connectionString;
        this.mongoClient = mongoClient;
        this.collection = getOrCreateMongoCollection(mongoDatabase, connectionString.getCollection(), isCapped,
                sizeInBytes);
    }

    @Override
    public void closeImpl() {
        // LOG4J2-1196
        mongoClient.close();
    }

    @Override
    public MongoDb4DocumentObject[] createList(final int length) {
        return new MongoDb4DocumentObject[length];
    }

    @Override
    public MongoDb4DocumentObject createObject() {
        return new MongoDb4DocumentObject();
    }

    @Override
    public void insertObject(final NoSqlObject<Document> object) {
        try {
            final Document unwrapped = object.unwrap();
            LOGGER.debug("Inserting BSON Document {}", unwrapped);
            InsertOneResult insertOneResult = this.collection.insertOne(unwrapped);
            LOGGER.debug("Insert MongoDb result {}", insertOneResult);
        } catch (final MongoException e) {
            throw new AppenderLoggingException("Failed to write log event to MongoDB due to error: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public String toString() {
        return String.format("Mongo4Connection [connectionString=%s, collection=%s, mongoClient=%s]", connectionString,
                collection, mongoClient);
    }

}
