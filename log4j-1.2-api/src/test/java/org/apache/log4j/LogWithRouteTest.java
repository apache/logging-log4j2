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
package org.apache.log4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Test passing MDC values to the Routing appender.
 */
public class LogWithRouteTest {

    private static final String CONFIG = "log-RouteWithMDC.xml";

    @ClassRule
    public static final LoggerContextRule CTX = new LoggerContextRule(CONFIG);

    @Test
    public void testMDC() throws Exception {
        MDC.put("Type", "Service");
        MDC.put("Name", "John Smith");
        try {
            final Logger logger = Logger.getLogger("org.apache.test.logging");
            logger.debug("This is a test");
            final ListAppender listApp = (ListAppender) CTX.getAppender("List");
            assertThat(listApp).isNotNull();
            final List<String> msgs = listApp.getMessages();
            assertThat(msgs).describedAs("No messages received").isNotNull();
            assertThat(msgs).hasSize(1);
            assertThat(msgs.get(0).contains("Type=Service")).describedAs("Type is missing").isTrue();
            assertThat(msgs.get(0).contains("Name=John Smith")).describedAs("Name is missing").isTrue();
        } finally {
            MDC.remove("Type");
            MDC.remove("Name");
        }
    }
}
