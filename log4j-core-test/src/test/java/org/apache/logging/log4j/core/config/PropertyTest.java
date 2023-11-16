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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;

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

        // <Property name="emptyElementKey" />
        // <Property name="emptyAttributeKey" value="" />
        // <Property name="emptyAttributeKey2" value=""></Property>
        // <Property name="elementKey">elementValue</Property>
        // <Property name="attributeKey" value="attributeValue" />
        // <Property name="attributeWithEmptyElementKey" value="attributeValue2"></Property>
        // <Property name="bothElementAndAttributeKey" value="attributeValue3">elementValue3</Property>
        final String expect = "1=elementValue" + // ${sys:elementKey}
                ",2="
                + // ${sys:emptyElementKey}
                ",a="
                + // ${sys:emptyAttributeKey}
                ",b="
                + // ${sys:emptyAttributeKey2}
                ",3=attributeValue"
                + // ${sys:attributeKey}
                ",4=attributeValue2"
                + // ${sys:attributeWithEmptyElementKey}
                ",5=elementValue3,m=msg"; // ${sys:bothElementAndAttributeKey}
        assertEquals(expect, messages.get(0));
        app.clear();
    }

    @Test
    public void testPropertyValues() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final StrSubstitutor sub = ctx.getConfiguration().getStrSubstitutor();
        // <Property name="emptyElementKey" />
        // <Property name="emptyAttributeKey" value="" />
        // <Property name="emptyAttributeKey2" value=""></Property>
        // <Property name="elementKey">elementValue</Property>
        // <Property name="attributeKey" value="attributeValue" />
        // <Property name="attributeWithEmptyElementKey" value="attributeValue2"></Property>
        // <Property name="bothElementAndAttributeKey" value="attributeValue3">elementValue3</Property>
        assertEquals("", sub.replace("${emptyElementKey}"));
        assertEquals("", sub.replace("${emptyAttributeKey}"));
        assertEquals("", sub.replace("${emptyAttributeKey2}"));
        assertEquals("elementValue", sub.replace("${elementKey}"));
        assertEquals("attributeValue", sub.replace("${attributeKey}"));
        assertEquals("attributeValue2", sub.replace("${attributeWithEmptyElementKey}"));
        assertEquals("elementValue3", sub.replace("${bothElementAndAttributeKey}"));
    }

    @Test
    public void testLoggerPropertyValues() throws Exception {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final List<Property> rootLoggerProperties =
                ctx.getConfiguration().getLoggerConfig(LoggerConfig.ROOT).getPropertyList();
        // <Property name="emptyElementKey" />
        // <Property name="emptyAttributeKey" value="" />
        // <Property name="emptyAttributeKey2" value=""></Property>
        // <Property name="elementKey">elementValue</Property>
        // <Property name="attributeKey" value="attributeValue" />
        // <Property name="attributeWithEmptyElementKey" value="attributeValue2"></Property>
        // <Property name="bothElementAndAttributeKey" value="attributeValue3">elementValue3</Property>
        assertEquals(9, rootLoggerProperties.size());
        verifyProperty(rootLoggerProperties.get(0), "emptyElementKey", "", "");
        verifyProperty(rootLoggerProperties.get(1), "emptyAttributeKey", "", "");
        verifyProperty(rootLoggerProperties.get(2), "emptyAttributeKey2", "", "");
        verifyProperty(rootLoggerProperties.get(3), "elementKey", "elementValue", "elementValue");
        verifyProperty(rootLoggerProperties.get(4), "attributeKey", "attributeValue", "attributeValue");
        verifyProperty(
                rootLoggerProperties.get(5), "attributeWithEmptyElementKey", "attributeValue2", "attributeValue2");
        verifyProperty(rootLoggerProperties.get(6), "bothElementAndAttributeKey", "elementValue3", "elementValue3");
        verifyProperty(rootLoggerProperties.get(7), "attributeWithLookup", "${lower:ATTR}", "attr");
        verifyProperty(rootLoggerProperties.get(8), "elementWithLookup", "${lower:ELEMENT}", "element");
    }

    private static void verifyProperty(
            final Property property,
            final String expectedName,
            final String expectedRawValue,
            final String expectedValue) {
        assertEquals(expectedName, property.getName());
        assertEquals(expectedRawValue, property.getRawValue());
        assertEquals(expectedValue, property.getValue());
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
