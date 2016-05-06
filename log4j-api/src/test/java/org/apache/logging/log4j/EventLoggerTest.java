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
package org.apache.logging.log4j;

import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 *
 */
public class EventLoggerTest {

    TestLogger logger = (TestLogger) LogManager.getLogger("EventLogger");
    List<String> results = logger.getEntries();

    @Before
    public void setup() {
        results.clear();
    }

    @Test
    public void structuredData() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Transfer@18060", "Transfer Complete", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        EventLogger.logEvent(msg);
        ThreadContext.clearMap();
        assertEquals(1, results.size());
        final String expected = "EVENT OFF Audit [Transfer@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"] Transfer Complete";
        assertThat("Incorrect structured data", results.get(0), startsWith(expected));
    }

}
