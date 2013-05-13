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
package org.apache.logging.log4j.core.appender.db.nosql.mongo;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.db.nosql.NoSQLConnection;
import org.apache.logging.log4j.core.appender.db.nosql.NoSQLObject;
import org.apache.logging.log4j.status.StatusLogger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * The MongoDB implementation of {@link NoSQLConnection}.
 */
public final class MongoDBConnection implements NoSQLConnection<BasicDBObject, MongoDBObject> {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final DBCollection collection;
    private final Mongo mongo;
    private final WriteConcern writeConcern;

    public MongoDBConnection(final DB database, final WriteConcern writeConcern, final String collectionName) {
        this.mongo = database.getMongo();
        this.collection = database.getCollection(collectionName);
        this.writeConcern = writeConcern;
    }

    @Override
    public MongoDBObject createObject() {
        return new MongoDBObject();
    }

    @Override
    public MongoDBObject[] createList(final int length) {
        return new MongoDBObject[length];
    }

    @Override
    public void insertObject(final NoSQLObject<BasicDBObject> object) {
        try {
            final WriteResult result = this.collection.insert(object.unwrap(), this.writeConcern);
            if (result.getN() < 1) {
                LOGGER.error("Failed to write log event to MongoDB due to invalid result [{}].", result.getN());
            }
        } catch (final MongoException e) {
            LOGGER.error("Failed to write log event to MongoDB due to error.", e);
        }
    }

    @Override
    public void close() {
        this.mongo.close();
    }

    @Override
    public boolean isClosed() {
        return !this.mongo.getConnector().isOpen();
    }
}
