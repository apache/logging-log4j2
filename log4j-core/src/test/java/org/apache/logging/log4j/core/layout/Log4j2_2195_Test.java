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

package org.apache.logging.log4j.core.layout;

import java.io.Serializable;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class Log4j2_2195_Test {

    @ClassRule
    public static final LoggerContextRule loggerContextRule = new LoggerContextRule(
            "src/test/resources/LOG4J-2195/log4j2.xml");

    private static final Logger logger = LogManager.getLogger(Log4j2_2195_Test.class);

    @Test
    public void test() {
        logger.info("This is a test.", new Exception("Test exception!"));
        ListAppender listAppender = loggerContextRule.getListAppender("ListAppender");
        Assert.assertNotNull(listAppender);
        List<String> events = listAppender.getMessages();
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        String logEvent = events.get(0);
        Assert.assertNotNull(logEvent);
        Assert.assertFalse("\"org.junit\" should not be here", logEvent.contains("org.junit"));
        Assert.assertFalse("\"org.eclipse\" should not be here", logEvent.contains("org.eclipse"));
        //
        Layout<? extends Serializable> layout = listAppender.getLayout();
        PatternLayout pLayout = (PatternLayout) layout;
        Assert.assertNotNull(pLayout);
        Serializer eventSerializer = pLayout.getEventSerializer();
        Assert.assertNotNull(eventSerializer);
        //
        Assert.assertTrue("Missing \"|\"", logEvent.contains("|"));
    }
}
