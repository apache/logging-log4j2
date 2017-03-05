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
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GelfLayoutTest2 {

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("GelfLayoutTest2.xml");

    @Test
    public void gelfLayout() throws IOException {
        Logger logger = context.getLogger();
        logger.info("Message");
        String gelf = context.getListAppender("list").getMessages().get(0);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(gelf);
        assertEquals("Message", json.get("short_message").asText());
        assertEquals("myhost", json.get("host").asText());
        assertEquals("FOO", json.get("_foo").asText());
        assertEquals(new JavaLookup().getRuntime(), json.get("_runtime").asText());
    }

}

