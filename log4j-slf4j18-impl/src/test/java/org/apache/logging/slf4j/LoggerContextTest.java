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
package org.apache.logging.slf4j;

import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Tests cleanup of the LoggerContexts.
 */
public class LoggerContextTest {

    @Test
    public void testCleanup() throws Exception {
        Log4jLoggerFactory factory = (Log4jLoggerFactory) LoggerFactory.getILoggerFactory();
        factory.getLogger("test");
        Set<LoggerContext> set = factory.getLoggerContexts();
        LoggerContext ctx1 = set.toArray(new LoggerContext[0])[0];
        assertTrue("LoggerContext is not enabled for shutdown", ctx1 instanceof LifeCycle);
        ((LifeCycle) ctx1).stop();
        set = factory.getLoggerContexts();
        assertTrue("Expected no LoggerContexts", set.isEmpty());
    }
}
