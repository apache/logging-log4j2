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
package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Layouts.Json")
public class StackTraceElementMixInTest {

    @Test
    public void testLog4jJsonObjectMapper() throws Exception {
        this.roundtrip(new Log4jJsonObjectMapper());
    }

    @Test
    public void testLog4jYamlObjectMapper() throws Exception {
        this.roundtrip(new Log4jYamlObjectMapper());
    }

    /**
     * @param mapper
     * @throws JsonProcessingException
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    private void roundtrip(final ObjectMapper mapper)
            throws JsonProcessingException, IOException, JsonParseException, JsonMappingException {
        final StackTraceElement expected =
                new StackTraceElement("package.SomeClass", "someMethod", "SomeClass.java", 123);
        final String s = mapper.writeValueAsString(expected);
        final StackTraceElement actual = mapper.readValue(s, StackTraceElement.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testLog4jXmlObjectMapper() throws Exception {
        this.roundtrip(new Log4jXmlObjectMapper());
    }

    protected String aposToQuotes(final String json) {
        return json.replace("'", "\"");
    }

    @Test
    public void testFromJsonWithSimpleModule() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(StackTraceElement.class, new Log4jStackTraceElementDeserializer());
        mapper.registerModule(module);
        final StackTraceElement expected =
                new StackTraceElement("package.SomeClass", "someMethod", "SomeClass.java", 123);
        final String s = this.aposToQuotes(
                "{'class':'package.SomeClass','method':'someMethod','file':'SomeClass.java','line':123}");
        final StackTraceElement actual = mapper.readValue(s, StackTraceElement.class);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testFromJsonWithLog4jModule() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final boolean encodeThreadContextAsList = false;
        final SimpleModule module = new Log4jJsonModule(encodeThreadContextAsList, true, false, false);
        module.addDeserializer(StackTraceElement.class, new Log4jStackTraceElementDeserializer());
        mapper.registerModule(module);
        final StackTraceElement expected =
                new StackTraceElement("package.SomeClass", "someMethod", "SomeClass.java", 123);
        final String s = this.aposToQuotes(
                "{'class':'package.SomeClass','method':'someMethod','file':'SomeClass.java','line':123}");
        final StackTraceElement actual = mapper.readValue(s, StackTraceElement.class);
        Assertions.assertEquals(expected, actual);
    }
}
