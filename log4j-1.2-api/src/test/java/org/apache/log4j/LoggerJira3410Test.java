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
package org.apache.log4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.config.TestConfigurator;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.api.Test;

/**
 * Tests Jira3410.
 */
public class LoggerJira3410Test {

    @Test
    public void test() throws Exception {
        try (final LoggerContext loggerContext =
                TestConfigurator.configure("target/test-classes/log4j1-list.properties")) {
            final Logger logger = LogManager.getLogger("test");
            //
            final Map<Object, Integer> map = new HashMap<>(1);
            map.put(Long.MAX_VALUE, 1);
            logger.debug(map);
            //
            map.put(null, null);
            logger.debug(map);
            //
            logger.debug(new SortedArrayStringMap((Map) map));
            //
            final Configuration configuration = loggerContext.getConfiguration();
            final Map<String, Appender> appenders = configuration.getAppenders();
            ListAppender listAppender = null;
            for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
                if (entry.getKey().equals("list")) {
                    listAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                }
            }
            assertNotNull(listAppender, "No Message Appender");
            final List<String> messages = listAppender.getMessages();
            assertTrue(messages != null && !messages.isEmpty(), "No messages");
            final String msg0 = messages.get(0);
            final String msg1 = messages.get(1);
            final String msg2 = messages.get(2);
            // TODO Should be 1, not "1".
            // TODO Where are the {} characters?
            assertTrue(msg0.trim().endsWith(Long.MAX_VALUE + "=\"1\""), msg0);
            //
            // TODO Should be 1, not "1".
            // TODO Should be null, not "null".
            // TODO Where are the {} characters?
            // TODO Where is the , characters?
            assertTrue(msg1.trim().endsWith("null=\"null\" " + Long.MAX_VALUE + "=\"1\""), msg1);
            //
            assertTrue(msg2.trim().endsWith("{null=null, " + Long.MAX_VALUE + "=1}"), msg2);
        }
    }
}
