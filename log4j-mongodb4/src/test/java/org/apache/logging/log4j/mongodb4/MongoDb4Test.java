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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.AvailablePortSystemPropertyTestRule;
import org.apache.logging.log4j.core.test.RuleChainFactory;
import org.apache.logging.log4j.core.test.categories.Appenders;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.mongodb4.MongoDb4TestRule.LoggingTarget;
import org.bson.Document;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 *
 */
@Category(Appenders.MongoDb.class)
public class MongoDb4Test {

    private static LoggerContextRule loggerContextTestRule = new LoggerContextRule("log4j2-mongodb.xml");

    private static final AvailablePortSystemPropertyTestRule mongoDbPortTestRule = AvailablePortSystemPropertyTestRule
            .create(MongoDb4TestConstants.SYS_PROP_NAME_PORT);

    private static final MongoDb4TestRule mongoDbTestRule = new MongoDb4TestRule(mongoDbPortTestRule.getName(),
            MongoDb4Test.class, LoggingTarget.NULL);

    @ClassRule
    public static RuleChain ruleChain = RuleChainFactory.create(mongoDbPortTestRule, mongoDbTestRule,
            loggerContextTestRule);

    @Test
    public void test() {
        final Logger logger = LogManager.getLogger();
        logger.info("Hello log 1");
        logger.info("Hello log 2", new RuntimeException("Hello ex 2"));
        try (final MongoClient mongoClient = mongoDbTestRule.getMongoClient()) {
            final MongoDatabase database = mongoClient.getDatabase(MongoDb4TestConstants.DATABASE_NAME);
            Assert.assertNotNull(database);
            final MongoCollection<Document> collection = database.getCollection(MongoDb4TestConstants.COLLECTION_NAME);
            Assert.assertNotNull(collection);
            final FindIterable<Document> found = collection.find();
            final Document first = found.first();
            Assert.assertNotNull("first", first);
            Assert.assertEquals(first.toJson(), "Hello log 1", first.getString("message"));
            Assert.assertEquals(first.toJson(), "INFO", first.getString("level"));
            //
            found.skip(1);
            final Document second = found.first();
            Assert.assertNotNull(second);
            Assert.assertEquals(second.toJson(), "Hello log 2", second.getString("message"));
            Assert.assertEquals(second.toJson(), "INFO", second.getString("level"));
            Document thrown = second.get("thrown", Document.class);
            Assert.assertEquals(thrown.toJson(), "Hello ex 2", thrown.getString("message"));
        }
    }
}
