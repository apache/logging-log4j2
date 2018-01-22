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
package org.apache.logging.log4j.mongodb2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.nosql.AbstractNoSqlConnection;
import org.apache.logging.log4j.core.appender.nosql.NoSqlConnection;
import org.apache.logging.log4j.core.appender.nosql.NoSqlObject;
import org.apache.logging.log4j.status.StatusLogger;
import org.bson.BSON;
import org.bson.Transformer;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

/**
 * The MongoDB implementation of {@link NoSqlConnection}.
 */
public final class MongoDbConnection extends AbstractNoSqlConnection<BasicDBObject, MongoDbObject> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    static {
        BSON.addEncodingHook(Level.class, new Transformer() {
            @Override
            public Object transform(final Object o) {
                if (o instanceof Level) {
                    return ((Level) o).name();
                }
                return o;
            }
        });
    }

    private final DBCollection collection;
    private final WriteConcern writeConcern;

    public MongoDbConnection(final DB database, final WriteConcern writeConcern, final String collectionName,
            final Boolean isCapped, final Integer collectionSize) {
        if (database.collectionExists(collectionName)) {
            LOGGER.debug("Gettting collection {}", collectionName);
            collection = database.getCollection(collectionName);
        } else {
            final BasicDBObject options = new BasicDBObject();
            options.put("capped", isCapped);
            options.put("size", collectionSize);
            LOGGER.debug("Creating collection {} (capped = {}, size = {})", collectionName, isCapped, collectionSize);
            this.collection = database.createCollection(collectionName, options);
        }
        this.writeConcern = writeConcern;
    }

    @Override
    public void closeImpl() {
        // LOG4J2-1196
        final Mongo mongo = this.collection.getDB().getMongo();
        LOGGER.debug("Closing {} client {}", mongo.getClass().getSimpleName(), mongo);
        mongo.close();
    }

    @Override
    public MongoDbObject[] createList(final int length) {
        return new MongoDbObject[length];
    }

    @Override
    public MongoDbObject createObject() {
        return new MongoDbObject();
    }

    @Override
    public void insertObject(final NoSqlObject<BasicDBObject> object) {
        try {
            final BasicDBObject unwrapped = object.unwrap();
            LOGGER.debug("Inserting object {}", unwrapped);
            this.collection.insert(unwrapped, this.writeConcern);
        } catch (final MongoException e) {
            throw new AppenderLoggingException("Failed to write log event to MongoDB due to error: " + e.getMessage(),
                    e);
        }
    }

}
