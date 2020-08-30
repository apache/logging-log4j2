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
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for LOG4J2-1313
 *  <Property name="" value="" /> not working
 */
@LoggerContextSource("configPropertyTest.xml")
public class PropertyTest {

    @Test
    public void testEmptyAttribute(@Named("List") final ListAppender app) throws Exception {
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
        logger.info("msg");

        final List<String> messages = app.getMessages();
        assertNotNull(messages, "No Messages");
        assertEquals(1, messages.size(), "message count" + messages);

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
        assertTrue(Property.createProperty("", "${").isValueNeedsLookup(), "with ${ as value");
        assertTrue(Property.createProperty("", "blah${blah").isValueNeedsLookup(), "with ${ in value");
        assertFalse(Property.createProperty("", "").isValueNeedsLookup(), "empty value");
        assertFalse(Property.createProperty("", "blahblah").isValueNeedsLookup(), "without ${ in value");
        assertFalse(Property.createProperty("", "blahb{sys:lah").isValueNeedsLookup(), "without $ in value");
        assertFalse(Property.createProperty("", "blahb$sys:lah").isValueNeedsLookup(), "without { in value");
    }
}
