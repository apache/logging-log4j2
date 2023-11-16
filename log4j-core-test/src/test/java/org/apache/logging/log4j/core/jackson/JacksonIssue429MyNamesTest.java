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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.io.IOException;
import org.apache.logging.log4j.core.test.categories.Layouts;
import org.apache.logging.log4j.util.Strings;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Layouts.Json.class)
public class JacksonIssue429MyNamesTest {

    @SuppressWarnings("serial")
    static class MyStackTraceElementDeserializer extends StdScalarDeserializer<StackTraceElement> {
        private static final long serialVersionUID = 1L;

        public static final MyStackTraceElementDeserializer instance = new MyStackTraceElementDeserializer();

        public MyStackTraceElementDeserializer() {
            super(StackTraceElement.class);
        }

        @Override
        public StackTraceElement deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonToken t = jp.getCurrentToken();
            // Must get an Object
            if (t == JsonToken.START_OBJECT) {
                String className = Strings.EMPTY, methodName = Strings.EMPTY, fileName = Strings.EMPTY;
                int lineNumber = -1;

                while ((t = jp.nextValue()) != JsonToken.END_OBJECT) {
                    final String propName = jp.getCurrentName();
                    if ("class".equals(propName)) {
                        className = jp.getText();
                    } else if ("file".equals(propName)) {
                        fileName = jp.getText();
                    } else if ("line".equals(propName)) {
                        if (t.isNumeric()) {
                            lineNumber = jp.getIntValue();
                        } else {
                            throw JsonMappingException.from(
                                    jp, "Non-numeric token (" + t + ") for property 'lineNumber'");
                        }
                    } else if ("method".equals(propName)) {
                        methodName = jp.getText();
                    } else if ("nativeMethod".equals(propName)) {
                        // no setter, not passed via constructor: ignore
                    } else {
                        handleUnknownProperty(jp, ctxt, _valueClass, propName);
                    }
                }
                return new StackTraceElement(className, methodName, fileName, lineNumber);
            }
            throw JsonMappingException.from(
                    jp,
                    String.format(
                            "Cannot deserialize instance of %s out of %s token",
                            ClassUtil.nameOf(this._valueClass), t));
        }
    }

    static class StackTraceBean {
        public static final int NUM = 13;

        @JsonProperty("Location")
        @JsonDeserialize(using = MyStackTraceElementDeserializer.class)
        private StackTraceElement location;
    }

    private static final ObjectMapper SHARED_MAPPER = new ObjectMapper();

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
        final StackTraceBean bean = MAPPER.readValue(
                aposToQuotes(
                        "{'Location':{'class':'package.SomeClass','method':'someMethod','file':'SomeClass.java','line':13}}"),
                StackTraceBean.class);
        Assert.assertNotNull(bean);
        Assert.assertNotNull(bean.location);
        Assert.assertEquals(StackTraceBean.NUM, bean.location.getLineNumber());

        // and then directly, iff registered
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(StackTraceElement.class, new MyStackTraceElementDeserializer());
        mapper.registerModule(module);

        final StackTraceElement elem = mapper.readValue(
                aposToQuotes("{'class':'package.SomeClass','method':'someMethod','file':'SomeClass.java','line':13}"),
                StackTraceElement.class);
        Assert.assertNotNull(elem);
        Assert.assertEquals(StackTraceBean.NUM, elem.getLineNumber());
    }
}
