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
package org.apache.logging.log4j.core.lookup;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.junit.Assert.*;

/**
 *
 */
public class ContextMapLookupTest {

    private static final String TESTKEY = "TestKey";
    private static final String TESTVAL = "TestValue";

    private final LoggerContextRule context = new LoggerContextRule("ContextMapLookupTest.xml");

    @Rule
    public RuleChain chain = RuleChain.outerRule(new TestRule() {
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final File logFile = new File("target",
                        description.getClassName() + '.' + description.getMethodName() + ".log");
                    ThreadContext.put("testClassName", description.getClassName());
                    ThreadContext.put("testMethodName", description.getMethodName());
                    try {
                        base.evaluate();
                    } finally {
                        ThreadContext.remove("testClassName");
                        ThreadContext.remove("testMethodName");
                        if (logFile.exists()) {
                            logFile.deleteOnExit();
                        }
                    }
                }
            };
        }
    }).around(context);

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
    public void testFileLog() throws Exception {
        final Logger logger = LogManager.getLogger();
        logger.info("Hello from testFileLog!");
        final File logFile = new File("target", this.getClass().getName() + ".testFileLog.log");
        assertTrue(logFile.exists());
    }
}
