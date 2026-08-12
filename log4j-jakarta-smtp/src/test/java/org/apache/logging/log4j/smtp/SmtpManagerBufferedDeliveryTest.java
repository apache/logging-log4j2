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
package org.apache.logging.log4j.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

class SmtpManagerBufferedDeliveryTest {

    @Test
    void bufferedEventsAreIncludedInSingleEmail() throws Exception {
        final CapturingSmtpManager manager =
                SmtpTestSupport.createCapturingManager(SmtpTestSupport.createDefaultFactoryData());
        final PatternLayout layout = SmtpTestSupport.createLayout();

        manager.add(SmtpTestSupport.createLogEvent("buffered-1"));
        manager.add(SmtpTestSupport.createLogEvent("buffered-2"));
        manager.sendEvents(layout, SmtpTestSupport.createLogEvent("trigger"));

        assertEquals(1, manager.getCapturedEmails().size());
        final String body = manager.getCapturedEmails().get(0).getBody();

        assertTrue(body.contains("buffered-1"));
        assertTrue(body.contains("buffered-2"));
        assertTrue(body.contains("trigger"));
    }

    @Test
    void bufferedEventsAreClearedAfterSend() throws Exception {
        final CapturingSmtpManager manager =
                SmtpTestSupport.createCapturingManager(SmtpTestSupport.createDefaultFactoryData());
        final PatternLayout layout = SmtpTestSupport.createLayout();

        manager.add(SmtpTestSupport.createLogEvent("first-buffer"));
        manager.sendEvents(layout, SmtpTestSupport.createLogEvent("first-trigger"));
        manager.sendEvents(layout, SmtpTestSupport.createLogEvent("second-trigger"));

        assertEquals(2, manager.getCapturedEmails().size());
        final String firstBody = manager.getCapturedEmails().get(0).getBody();
        final String secondBody = manager.getCapturedEmails().get(1).getBody();

        assertTrue(firstBody.contains("first-buffer"));
        assertTrue(firstBody.contains("first-trigger"));
        assertFalse(secondBody.contains("first-buffer"));
        assertTrue(secondBody.contains("second-trigger"));
    }

    @Test
    void appendEventIsSentEvenWhenBufferIsEmpty() throws Exception {
        final CapturingSmtpManager manager =
                SmtpTestSupport.createCapturingManager(SmtpTestSupport.createDefaultFactoryData());
        final PatternLayout layout = SmtpTestSupport.createLayout();

        manager.sendEvents(layout, SmtpTestSupport.createLogEvent("only-trigger"));

        assertEquals(1, manager.getCapturedEmails().size());
        assertTrue(manager.getCapturedEmails().get(0).getBody().contains("only-trigger"));
    }
}
