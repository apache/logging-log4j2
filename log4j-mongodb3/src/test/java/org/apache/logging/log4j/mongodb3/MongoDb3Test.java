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
package org.apache.logging.log4j.mongodb3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.bson.Document;
import org.junit.jupiter.api.Test;

@UsingMongoDb3
@LoggerContextSource("log4j2-mongodb.xml")
public class MongoDb3Test {

    @Test
    public void test(final LoggerContext ctx, final MongoClient mongoClient) {
        final Logger logger = ctx.getLogger(MongoDb3Test.class);
        logger.info("Hello log 1");
        logger.info("Hello log 2", new RuntimeException("Hello ex 2"));
        final MongoDatabase database = mongoClient.getDatabase(MongoDb3TestConstants.DATABASE_NAME);
        assertNotNull(database);
        final MongoCollection<Document> collection = database.getCollection(MongoDb3TestConstants.COLLECTION_NAME);
        assertNotNull(collection);
        final FindIterable<Document> found = collection.find();
        final Document first = found.first();
        assertNotNull(first, "first");
        assertEquals("Hello log 1", first.getString("message"), first.toJson());
        assertEquals("INFO", first.getString("level"), first.toJson());
        //
        found.skip(1);
        final Document second = found.first();
        assertNotNull(second);
        assertEquals("Hello log 2", second.getString("message"), second.toJson());
        assertEquals("INFO", second.getString("level"), second.toJson());
        final Document thrown = second.get("thrown", Document.class);
        assertEquals("Hello ex 2", thrown.getString("message"), thrown.toJson());
    }
}
