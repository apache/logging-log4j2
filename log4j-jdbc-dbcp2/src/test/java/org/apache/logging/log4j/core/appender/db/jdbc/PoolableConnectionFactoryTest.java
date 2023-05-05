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
package org.apache.logging.log4j.core.appender.db.jdbc;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class PoolableConnectionFactoryTest {

    private static final String REL_PATH = "src/test/resources/log4j2-jdbc-dbcp2.xml";

    @ClassRule
    public static final LoggerContextRule LCR = LoggerContextRule.createShutdownTimeoutLoggerContextRule(REL_PATH);

    @Test
    public void test() {
        final Appender appender = LCR.getAppender("databaseAppender");
        Assert.assertNotNull("Problem loading configuration from " + REL_PATH, appender);
    }
}
