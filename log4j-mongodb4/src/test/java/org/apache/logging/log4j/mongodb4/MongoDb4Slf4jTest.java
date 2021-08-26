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

import org.apache.logging.log4j.core.test.categories.Appenders;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.mongodb4.MongoDb4TestRule.LoggingTarget;
import org.apache.logging.log4j.core.test.AvailablePortSystemPropertyTestRule;
import org.apache.logging.log4j.core.test.RuleChainFactory;
import org.bson.Document;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 *
 */
@Category(Appenders.MongoDb.class)
public class MongoDb4Slf4jTest {
	
    private static final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    private static LoggerContextRule loggerContextTestRule = new LoggerContextRule("log4j2-mongodb-slf4j.xml");

    @ClassRule
    public static RuleChain ruleChain = RuleChainFactory.create(systemOutRule, loggerContextTestRule);

    @Test
    public void test() {
        final Logger logger = LoggerFactory.getLogger(MongoDb4Slf4jTest.class);
        logger.info("Hello log");
        try (final MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            final MongoDatabase database = mongoClient.getDatabase("testDb");
            Assert.assertNotNull(database);
            final MongoCollection<Document> collection = database.getCollection("testCollection");
            Assert.assertNotNull(collection);
            final Document first = collection.find().first();
            Assert.assertNotNull(first);
            Assert.assertEquals(first.toJson(), "Hello log", first.getString("message"));
            Assert.assertEquals(first.toJson(), "INFO", first.getString("level"));
            Assert.assertTrue(!systemOutRule.getLog().contains("Recursive call to appender "));
        }
    }
}
