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

import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Test;

import java.util.Date;
import java.util.Locale;

/**
 *
 */
public class LoggerTest {

    Logger logger = LogManager.getLogger("LoggerTest");
    @Test
    public void basicFlow() {
        logger.entry();
        logger.exit();
    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (Exception e) {
            logger.catching(e);
        }
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
    }

    @Test
    public void mdc() {

        MDC.put("TestYear", new Integer(2010));
        logger.debug("Debug message");
        MDC.clear();
        logger.debug("Debug message");
    }

    @Test
    public void structuredData() {
        MDC.put("loginId", "JohnDoe");
        MDC.put("ipAddress", "192.168.0.120");
        MDC.put("locale", Locale.US.getDisplayName());
        StructuredDataMessage msg = new StructuredDataMessage("Audit@18060", "Transfer Complete", "Transfer");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        logger.info(Marker.getMarker("EVENT"), msg);
        MDC.clear();
    }
}
