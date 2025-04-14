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
package org.apache.logging.log4j.core.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ContextMapLookupTest {

    private static final String TESTKEY = "TestKey";
    private static final String TESTVAL = "TestValue";

    private static File logFile;

    @BeforeAll
    private static void before() {
        String className = ContextMapLookupTest.class.getName();
        String methodName = "testFileLog";

        logFile = new File("target", className + '.' + methodName + ".log");
        ThreadContext.put("testClassName", className);
        ThreadContext.put("testMethodName", methodName);
    }

    @AfterAll
    private static void after() {
        ThreadContext.remove("testClassName");
        ThreadContext.remove("testMethodName");

        if (logFile.exists()) {
            logFile.deleteOnExit();
        }
    }

    @Test
    public void testLookup() {
        ThreadContext.put(TESTKEY, TESTVAL);
        final StrLookup lookup = new ContextMapLookup();
        String value = lookup.lookup(TESTKEY);
        assertEquals(TESTVAL, value);
        value = lookup.lookup("BadKey");
        assertNull(value);
    }

    /**
     * Demonstrates the use of ThreadContext in determining what log file name to use in a unit test.
     * Inspired by LOG4J2-786. Note that this only works because the ThreadContext is prepared
     * <em>before</em> the Logger instance is obtained. This use of ThreadContext and the associated
     * ContextMapLookup can be used in many other ways in a config file.
     */
    @Test
    @LoggerContextSource("ContextMapLookupTest.xml")
    public void testFileLog() {
        final Logger logger = LogManager.getLogger();
        logger.info("Hello from testFileLog!");
        final File logFile = new File("target", this.getClass().getName() + ".testFileLog.log");
        assertTrue(logFile.exists());
    }
}
