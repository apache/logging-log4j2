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
package org.apache.logging.log4j.core.layout;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@LoggerContextSource("GelfLayout3Test.xml")
@UsingAnyThreadContext
@Tag("json")
public class GelfLayout3Test {

    @Test
    public void gelfLayout(final LoggerContext context, @Named final ListAppender list) throws IOException {
        list.clear();
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put("loginId", "rgoers");
        ThreadContext.put("internalId", "12345");
        logger.info("My Test Message");
        final String gelf = list.getMessages().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode json = mapper.readTree(gelf);
        assertEquals("My Test Message", json.get("short_message").asText());
        assertEquals("myhost", json.get("host").asText());
        assertNotNull(json.get("_mdc.loginId"));
        assertEquals("rgoers", json.get("_mdc.loginId").asText());
        assertNull(json.get("_mdc.internalId"));
        assertNull(json.get("_mdc.requestId"));
        final String message = json.get("full_message").asText();
        assertTrue(message.contains("loginId=rgoers"));
        assertTrue(message.contains("GelfLayout3Test"));
        assertNull(json.get("_map.arg1"));
        assertNull(json.get("_map.arg2"));
        assertNull(json.get("_empty"));
        assertEquals("FOO", json.get("_foo").asText());
    }

    @Test
    public void mapMessage(final LoggerContext context, @Named final ListAppender list) throws IOException {
        list.clear();
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put("loginId", "rgoers");
        ThreadContext.put("internalId", "12345");
        final StringMapMessage message = new StringMapMessage();
        message.put("arg1", "test1");
        message.put("arg2", "");
        message.put("arg3", "test3");
        message.put("message", "My Test Message");
        logger.info(message);
        final String gelf = list.getMessages().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode json = mapper.readTree(gelf);
        assertEquals(
                "arg1=\"test1\" arg2=\"\" arg3=\"test3\" message=\"My Test Message\"",
                json.get("short_message").asText());
        assertEquals("myhost", json.get("host").asText());
        assertNotNull(json.get("_mdc.loginId"));
        assertEquals("rgoers", json.get("_mdc.loginId").asText());
        assertNull(json.get("_mdc.internalId"));
        assertNull(json.get("_mdc.requestId"));
        final String msg = json.get("full_message").asText();
        assertTrue(msg.contains("loginId=rgoers"));
        assertTrue(msg.contains("GelfLayout3Test"));
        assertTrue(msg.contains("arg1=\"test1\""));
        assertNull(json.get("_map.arg2"));
        assertEquals("test1", json.get("_map.arg1").asText());
        assertEquals("test3", json.get("_map.arg3").asText());
        assertNull(json.get("_empty"));
        assertEquals("FOO", json.get("_foo").asText());
    }
}
