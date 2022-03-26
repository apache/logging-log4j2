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
package org.apache.logging.log4j.jackson.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.logging.log4j.jackson.Log4jStackTraceElementDeserializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StackTraceElementYamlMixInTest {

    protected String aposToQuotes(final String json) {
        return json.replace("'", "\"");
    }

    private void roundtrip(final ObjectMapper mapper) throws IOException {
        final StackTraceElement expected = new StackTraceElement("package.SomeClass", "someMethod", "SomeClass.java",
                123);
        final String s = mapper.writeValueAsString(expected);
        final StackTraceElement actual = mapper.readValue(s, StackTraceElement.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testFromYamlWithLog4jModule() throws Exception {
        final ObjectMapper mapper = new YAMLMapper();
        final boolean encodeThreadContextAsList = false;
        final SimpleModule module = new Log4jYamlModule(encodeThreadContextAsList, true, false);
        module.addDeserializer(StackTraceElement.class, new Log4jStackTraceElementDeserializer());
        mapper.registerModule(module);
        final StackTraceElement expected = new StackTraceElement("package.SomeClass", "someMethod", "SomeClass.java",
                123);
        final StackTraceElement actual = mapper.readValue(
                "---\nclass: package.SomeClass\nmethod: someMethod\nfile: SomeClass.java\nline: 123\n...",
                StackTraceElement.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testFromYamlWithSimpleModule() throws Exception {
        final ObjectMapper mapper = new YAMLMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(StackTraceElement.class, new Log4jStackTraceElementDeserializer());
        mapper.registerModule(module);
        final StackTraceElement expected = new StackTraceElement("package.SomeClass", "someMethod", "SomeClass.java",
                123);
        final StackTraceElement actual = mapper.readValue(
                "---\nclass: package.SomeClass\nmethod: someMethod\nfile: SomeClass.java\nline: 123\n...",
                StackTraceElement.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testLog4jYamlObjectMapper() throws Exception {
        this.roundtrip(new Log4jYamlObjectMapper());
    }
}
