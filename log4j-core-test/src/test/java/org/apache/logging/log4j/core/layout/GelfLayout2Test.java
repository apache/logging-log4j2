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

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("GelfLayout2Test.xml")
public class GelfLayout2Test {

    @Test
    public void gelfLayout(final LoggerContext context, @Named final ListAppender list) throws IOException {
        list.clear();
        final Logger logger = context.getLogger(getClass());
        logger.info("Message");
        final String gelf = list.getMessages().get(0);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode json = mapper.readTree(gelf);
        assertEquals("Message", json.get("short_message").asText());
        assertEquals("myhost", json.get("host").asText());
        assertEquals("FOO", json.get("_foo").asText());
        assertEquals(new JavaLookup().getRuntime(), json.get("_runtime").asText());
    }

}

