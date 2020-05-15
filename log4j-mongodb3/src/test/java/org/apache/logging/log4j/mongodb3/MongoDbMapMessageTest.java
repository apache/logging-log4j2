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
package org.apache.logging.log4j.mongodb3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.mongodb3.MongoDbTestRule.LoggingTarget;
import org.apache.logging.log4j.test.AvailablePortSystemPropertyTestRule;
import org.apache.logging.log4j.test.RuleChainFactory;
import org.bson.Document;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 *
 */
@Category(Appenders.MongoDb.class)
public class MongoDbMapMessageTest {

    private static LoggerContextRule loggerContextTestRule = new LoggerContextRule("log4j2-mongodb-map-message.xml");

    private static final AvailablePortSystemPropertyTestRule mongoDbPortTestRule = AvailablePortSystemPropertyTestRule
            .create(TestConstants.SYS_PROP_NAME_PORT);

    private static final MongoDbTestRule mongoDbTestRule = new MongoDbTestRule(mongoDbPortTestRule.getName(),
            MongoDbMapMessageTest.class, LoggingTarget.NULL);

    @ClassRule
    public static RuleChain ruleChain = RuleChainFactory.create(mongoDbPortTestRule, mongoDbTestRule,
            loggerContextTestRule);

    @Test
    public void test() {
        final Logger logger = LogManager.getLogger();
        final MapMessage<?, Object> mapMessage = new MapMessage<>();
        mapMessage.with("SomeName", "SomeValue");
        mapMessage.with("SomeInt", 1);
        logger.info(mapMessage);
        //
        try (final MongoClient mongoClient = mongoDbTestRule.getMongoClient()) {
            final MongoDatabase database = mongoClient.getDatabase("test");
            Assert.assertNotNull(database);
            final MongoCollection<Document> collection = database.getCollection("applog");
            Assert.assertNotNull(collection);
            final Document first = collection.find().first();
            Assert.assertNotNull(first);
            final String firstJson = first.toJson();
            Assert.assertEquals(firstJson, "SomeValue", first.getString("SomeName"));
            Assert.assertEquals(firstJson, Integer.valueOf(1), first.getInteger("SomeInt"));
        }
    }
}
