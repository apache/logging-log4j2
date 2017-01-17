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
package org.apache.logging.log4j.core.config.xml;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 *
 */
public class XmlLoggerPropsTest {

    private static final String CONFIG = "log4j-loggerprops.xml";

    @Rule
    public final LoggerContextRule context = new LoggerContextRule(CONFIG);

    @BeforeClass
    public static void setupClass() {
        System.setProperty("test", "test");
    }

    @Test
    public void testWithProps() {
        final ListAppender listAppender = context.getListAppender("List");
        assertNotNull("No List Appender", listAppender);

        try {
            assertThat(context.getConfiguration(), is(instanceOf(XmlConfiguration.class)));
            Logger logger = LogManager.getLogger(XmlLoggerPropsTest.class);
            logger.debug("Test with props");
            logger = LogManager.getLogger("tiny.bubbles");
            logger.debug("Test on root");
            final List<String> events = listAppender.getMessages();
            assertTrue("No events", events.size() > 0);
            assertTrue("Incorrect number of events", events.size() == 2);
            assertThat(events.get(0), allOf(
                containsString("user="),
                containsString("phrasex=****"),
                containsString("test=test"),
                containsString("test2=test2default"),
                containsString("test3=Unknown"),
                containsString("test4=test"),
                containsString("test5=test"),
                containsString("attribKey=attribValue"),
                containsString("duplicateKey=nodeValue")
            ));
            assertThat(events.get(1), allOf(
                containsString("user="),
                containsString("phrasex=****"),
                containsString("test=test"),
                containsString("test2=test2default"),
                containsString("test3=Unknown"),
                containsString("test4=test"),
                containsString("test5=test"),
                containsString("attribKey=attribValue"),
                containsString("duplicateKey=nodeValue")
            ));
        } finally {
            System.clearProperty("test");
        }
    }
}
