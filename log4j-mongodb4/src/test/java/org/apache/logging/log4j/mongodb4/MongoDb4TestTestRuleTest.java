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

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.mongodb4.MongoDb4TestRule.LoggingTarget;
import org.apache.logging.log4j.test.AvailablePortSystemPropertyTestRule;
import org.apache.logging.log4j.test.RuleChainFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.mongodb.client.MongoIterable;

/**
 * Tests MongoDbRule.
 * <p>
 * The test framework {@code de.flapdoodle.embed.mongo} requires Java 8.
 * </p>
 */
public class MongoDb4TestTestRuleTest {

    private static final AvailablePortSystemPropertyTestRule mongoDbPortTestRule = AvailablePortSystemPropertyTestRule
            .create(MongoDb4TestConstants.SYS_PROP_NAME_PORT);

    private static final MongoDb4TestRule mongoDbTestRule = new MongoDb4TestRule(mongoDbPortTestRule.getName(),
            MongoDb4TestTestRuleTest.class, LoggingTarget.NULL);

    @ClassRule
    public static RuleChain mongoDbChain = RuleChainFactory.create(mongoDbPortTestRule, mongoDbTestRule);

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeTrue(SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8));
    }

    @Test
    public void testAccess() {
        @SuppressWarnings("resource")
        final MongoIterable<String> databaseNames = mongoDbTestRule.getMongoClient().listDatabaseNames();
        Assert.assertNotNull(databaseNames);
        Assert.assertNotNull(databaseNames.first());
    }

    @SuppressWarnings("resource")
    @Test
    public void testMongoDbTestRule() {
        Assert.assertNotNull(mongoDbTestRule);
        Assert.assertNotNull(mongoDbTestRule.getStarter());
        Assert.assertNotNull(mongoDbTestRule.getMongoClient());
        Assert.assertNotNull(mongoDbTestRule.getMongodExecutable());
        Assert.assertNotNull(mongoDbTestRule.getMongodProcess());
    }
}
