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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;

@LoggerContextSource(value = "log4j2-jdbc-dbcp2.xml", timeout = 10)
class PoolableConnectionFactoryTest {

    private static final String REL_PATH = "src/test/resources/log4j2-jdbc-dbcp2.xml";

    @Test
    void test(@Named("databaseAppender") final Appender appender) {
        assertNotNull(appender, "Problem loading configuration from " + REL_PATH);
    }
}
