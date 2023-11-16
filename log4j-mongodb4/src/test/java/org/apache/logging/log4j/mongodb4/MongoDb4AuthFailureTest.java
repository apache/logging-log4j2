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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.bson.Document;
import org.junit.jupiter.api.Test;

@UsingMongoDb4
@LoggerContextSource("log4j2-mongodb-auth-failure.xml")
public class MongoDb4AuthFailureTest {

    @Test
    public void test(final LoggerContext ctx, final MongoClient mongoClient) {
        final Logger logger = ctx.getLogger(MongoDb4AuthFailureTest.class);
        logger.info("Hello log");
        final MongoDatabase database = mongoClient.getDatabase(MongoDb4TestConstants.DATABASE_NAME);
        assertNotNull(database);
        final MongoCollection<Document> collection = database.getCollection(MongoDb4TestConstants.DATABASE_NAME);
        assertNotNull(collection);
        final Document first = collection.find().first();
        assertNull(first);
    }
}
