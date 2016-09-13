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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

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

        //<Property name="emptyElementKey" />
        //<Property name="emptyAttributeKey" value="" />
        //<Property name="emptyAttributeKey2" value=""></Property>
        //<Property name="elementKey">elementValue</Property>
        //<Property name="attributeKey" value="attributeValue" />
        //<Property name="attributeWithEmptyElementKey" value="attributeValue2"></Property>
        //<Property name="bothElementAndAttributeKey" value="attributeValue3">elementValue3</Property>
        final String expect = "1=elementValue" + // ${sys:elementKey}
                ",2=" + // ${sys:emptyElementKey}
                ",a=" + // ${sys:emptyAttributeKey}
                ",b=" + // ${sys:emptyAttributeKey2}
                ",3=attributeValue" + // ${sys:attributeKey}
                ",4=attributeValue2" + // ${sys:attributeWithEmptyElementKey}
                ",5=elementValue3,m=msg"; // ${sys:bothElementAndAttributeKey}
        assertEquals(expect, messages.get(0));
        app.clear();
    }

    @Test
    public void testNullValueIsConvertedToEmptyString() { // LOG4J2-1313 <Property name="x" /> support
        assertEquals("", Property.createProperty("name", null).getValue());
    }

    @Test
    public void testIsValueNeedsLookup() {
        assertTrue("with ${ as value", Property.createProperty("", "${").isValueNeedsLookup());
        assertTrue("with ${ in value", Property.createProperty("", "blah${blah").isValueNeedsLookup());
        assertFalse("empty value", Property.createProperty("", "").isValueNeedsLookup());
        assertFalse("without ${ in value", Property.createProperty("", "blahblah").isValueNeedsLookup());
        assertFalse("without $ in value", Property.createProperty("", "blahb{sys:lah").isValueNeedsLookup());
        assertFalse("without { in value", Property.createProperty("", "blahb$sys:lah").isValueNeedsLookup());
    }
}
