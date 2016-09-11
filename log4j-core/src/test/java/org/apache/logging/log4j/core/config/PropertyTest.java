package org.apache.logging.log4j.core.config;/*
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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for LOG4J2-1313
 *  <Property name="" value="" /> not working
 */
public class PropertyTest {
    private static final String CONFIG = "configPropertyTest.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void testEmptyAttribute() throws Exception {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
        logger.info("msg");

        final ListAppender app = (ListAppender) context.getRequiredAppender("List");
        assertNotNull("No ListAppender", app);

        final List<String> messages = app.getMessages();
        assertNotNull("No Messages", messages);
        assertEquals("message count" + messages, 1, messages.size());

//        <Property name="elementKey">elementValue</Property>
//        <Property name="emptyElementKey"></Property>
//        <Property name="attributeKey" value="attributeValue" />
//        <Property name="attributeWithEmptyElementKey" value="attributeValue2"></Property>
//        <Property name="bothElementAndAttributeKey" value="attributeValue"3>elementValue</Property>
        final String expect = "1=elementValue" + // ${sys:elementKey}
                ",2=" + // ${sys:emptyElementKey}
                ",3=attributeValue" + // ${sys:attributeKey}
                ",4=attributeValue2" + // ${sys:attributeWithEmptyElementKey}
                ",5=attributeValue3,m=msg"; // ${sys:bothElementAndAttributeKey}
        assertEquals(expect, messages.get(0));
        app.clear();
    }
}