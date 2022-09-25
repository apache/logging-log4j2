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
package org.apache.logging.log4j.core.jackson;

import java.io.IOException;

import org.apache.logging.log4j.categories.Layouts;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.experimental.categories.Category;

@Category(Layouts.Json.class)
public class JacksonIssue429Test {

    @SuppressWarnings("serial")
    static class Jackson429StackTraceElementDeserializer extends StdDeserializer<StackTraceElement> {
        private static final long serialVersionUID = 1L;

        public Jackson429StackTraceElementDeserializer() {
            super(StackTraceElement.class);
        }

        @Override
        public StackTraceElement deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            jp.skipChildren();
            return new StackTraceElement("a", "b", "b", StackTraceBean.NUM);
        }

    }

    static class StackTraceBean {
        public final static int NUM = 13;

        @JsonProperty("Location")
        @JsonDeserialize(using = Jackson429StackTraceElementDeserializer.class)
        private StackTraceElement location;
    }

    private final static ObjectMapper SHARED_MAPPER = new ObjectMapper();

    private final ObjectMapper MAPPER = objectMapper();

    protected String aposToQuotes(final String json) {
        return json.replace("'", "\"");
    }

    protected ObjectMapper objectMapper() {
        return SHARED_MAPPER;
    }

    @Test
    public void testStackTraceElementWithCustom() throws Exception {
        // first, via bean that contains StackTraceElement
        final StackTraceBean bean = MAPPER.readValue(aposToQuotes("{'Location':'foobar'}"), StackTraceBean.class);
        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.location);
        Assert.assertEquals(StackTraceBean.NUM, bean.location.getLineNumber());

        // and then directly, iff registered
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(StackTraceElement.class, new Jackson429StackTraceElementDeserializer());
        mapper.registerModule(module);

        final StackTraceElement elem = mapper.readValue(
                aposToQuotes("{'class':'package.SomeClass','method':'someMethod','file':'SomeClass.java','line':123}"),
                StackTraceElement.class);
        Assert.assertNotNull(elem);
        Assert.assertEquals(StackTraceBean.NUM, elem.getLineNumber());
    }
}
