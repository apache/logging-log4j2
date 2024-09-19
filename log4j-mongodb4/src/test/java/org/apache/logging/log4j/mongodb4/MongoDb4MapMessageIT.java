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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@UsingMongoDb4
@LoggerContextSource("MongoDb4MapMessageIT.xml")
// Print debug status logger output upon failure
@UsingStatusListener
class MongoDb4MapMessageIT {

    @Test
    void test(final LoggerContext ctx, final MongoClient mongoClient) {
        final Logger logger = ctx.getLogger(MongoDb4MapMessageIT.class);
        final MapMessage<?, Object> mapMessage = new MapMessage<>();
        mapMessage.with("SomeName", "SomeValue");
        mapMessage.with("SomeInt", 1);
        logger.info(mapMessage);
        final MongoDatabase database = mongoClient.getDatabase(MongoDb4TestConstants.DATABASE_NAME);
        Assertions.assertNotNull(database);
        final MongoCollection<Document> collection =
                database.getCollection(getClass().getSimpleName());
        Assertions.assertNotNull(collection);
        final Document first = collection.find().first();
        Assertions.assertNotNull(first);
        final String firstJson = first.toJson();
        Assertions.assertEquals("SomeValue", first.getString("SomeName"), firstJson);
        Assertions.assertEquals(Integer.valueOf(1), first.getInteger("SomeInt"), firstJson);
    }
}
