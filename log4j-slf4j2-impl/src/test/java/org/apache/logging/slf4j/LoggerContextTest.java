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
package org.apache.logging.slf4j;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Tests cleanup of the LoggerContexts.
 */
public class LoggerContextTest {

    @Test
    public void testCleanup() throws Exception {
        final Log4jLoggerFactory factory = (Log4jLoggerFactory) LoggerFactory.getILoggerFactory();
        factory.getLogger("test");
        Set<LoggerContext> set = factory.getLoggerContexts();
        final LoggerContext ctx1 = set.toArray(LoggerContext.EMPTY_ARRAY)[0];
        assertTrue(ctx1 instanceof LifeCycle, "LoggerContext is not enabled for shutdown");
        ((LifeCycle) ctx1).stop();
        set = factory.getLoggerContexts();
        assertTrue(set.isEmpty(), "Expected no LoggerContexts");
    }
}
