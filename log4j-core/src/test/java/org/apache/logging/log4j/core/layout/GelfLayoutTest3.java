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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GelfLayoutTest3 {

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("GelfLayoutTest3.xml");

    @After
    public void teardown() throws Exception {
        ThreadContext.clearMap();
    }

    @Test
    public void gelfLayout() throws IOException {
        final Logger logger = context.getLogger();
        ThreadContext.put("loginId", "rgoers");
        ThreadContext.put("internalId", "12345");
        logger.info("My Test Message");
        final String gelf = context.getListAppender("list").getMessages().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode json = mapper.readTree(gelf);
        assertEquals("My Test Message", json.get("short_message").asText());
        assertEquals("myhost", json.get("host").asText());
        assertNotNull(json.get("_loginId"));
        assertEquals("rgoers", json.get("_loginId").asText());
        assertNull(json.get("_internalId"));
        assertNull(json.get("_requestId"));
        String message = json.get("full_message").asText();
        assertTrue(message.contains("loginId=rgoers"));
        assertTrue(message.contains("GelfLayoutTest3"));
    }

}

