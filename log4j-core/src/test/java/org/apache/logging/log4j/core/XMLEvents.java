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
package org.apache.logging.log4j.core;

import java.util.Locale;

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class XMLEvents {

    private static final String CONFIG = "xml-events.xml";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
    }

    @Test
    public void testEvents() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final TransferMessage msg = new TransferMessage();
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        EventLogger.logEvent(msg);
        msg.setCompletionStatus("Transfer Complete");
        EventLogger.logEvent(msg);
        ThreadContext.clearMap();


    }

    private class TransferMessage extends StructuredDataMessage {

        /**
         * Generated serial version ID.
         */
        private static final long serialVersionUID = -4334703653495359785L;

        public TransferMessage() {
            super("Transfer@18060", null, "Audit");
        }

        public void setCompletionStatus(final String msg) {
            setMessageFormat(msg);
        }
    }
}
