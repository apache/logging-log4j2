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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@LoggerContextSource("GelfLayoutPatternSelectorTest.xml")
@UsingAnyThreadContext
@Tag("json")
public class GelfLayoutPatternSelectorTest {

    @Test
    public void gelfLayout(final LoggerContext context, @Named final ListAppender list) throws IOException {
        list.clear();
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put("loginId", "rgoers");
        ThreadContext.put("internalId", "12345");
        logger.info("My Test Message");
        logger.info(AbstractLogger.FLOW_MARKER, "My Test Message");
        String gelf = list.getMessages().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(gelf);
        assertEquals("My Test Message", json.get("short_message").asText());
        assertEquals("myhost", json.get("host").asText());
        assertNotNull(json.get("_loginId"));
        assertEquals("rgoers", json.get("_loginId").asText());
        assertNull(json.get("_internalId"));
        assertNull(json.get("_requestId"));
        String message = json.get("full_message").asText();
        assertFalse(message.contains("====="));
        assertTrue(message.contains("loginId=rgoers"));
        assertTrue(message.contains("GelfLayoutPatternSelectorTest"));
        gelf = list.getMessages().get(1);
        json = mapper.readTree(gelf);
        assertEquals("My Test Message", json.get("short_message").asText());
        assertEquals("myhost", json.get("host").asText());
        assertNotNull(json.get("_loginId"));
        assertEquals("rgoers", json.get("_loginId").asText());
        assertNull(json.get("_internalId"));
        assertNull(json.get("_requestId"));
        message = json.get("full_message").asText();
        assertTrue(message.contains("====="));
        assertTrue(message.contains("loginId=rgoers"));
        assertTrue(message.contains("GelfLayoutPatternSelectorTest"));
    }
}
