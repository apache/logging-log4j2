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
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.ThreadContextDataInjector;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("functional")
public class ContextDataProviderTest {

    private static Logger logger;
    private static ListAppender appender;

    @BeforeAll
    public static void beforeClass() {
        ThreadContextDataInjector.contextDataProviders.add(new TestContextDataProvider());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "log4j-contextData.xml");
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        logger = loggerContext.getLogger(ContextDataProviderTest.class.getName());
        appender = loggerContext.getConfiguration().getAppender("List");
        assertNotNull(appender, "No List appender");
    }

    @Test
    public void testContextProvider() {
        ThreadContext.put("loginId", "jdoe");
        logger.debug("This is a test");
        final List<String> messages = appender.getMessages();
        assertEquals(1, messages.size(), "Incorrect number of messages");
        assertTrue(messages.get(0).contains("testKey=testValue"), "Context data missing");
    }

    private static class TestContextDataProvider implements ContextDataProvider {

        @Override
        public Map<String, String> supplyContextData() {
            final Map<String, String> contextData = new HashMap<>();
            contextData.put("testKey", "testValue");
            return contextData;
        }
    }
}
