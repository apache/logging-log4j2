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

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs all MongoDB only on Java 8.
 * <p>
 * The test framework {@code de.flapdoodle.embed.mongo} requires Java 8.
 * </p>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ MongoDbTestTestRuleTestJava8.class, MongoDbAuthFailureTestJava8.class, MongoDbCappedTestJava8.class,
        MongoDbMapMessageTestJava8.class, MongoDbTestJava8.class })
public class Java8Test {

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeTrue(SystemUtils.JAVA_VERSION, SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8));
    }

    @Test
    public void test() {
        // noop
    }
}
