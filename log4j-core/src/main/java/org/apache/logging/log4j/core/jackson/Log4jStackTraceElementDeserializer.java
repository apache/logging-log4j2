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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Copy and edit the Jackson (Apache License 2.0) class to use Log4j attribute names. Does not work as of Jackson 2.3.2.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public final class Log4jStackTraceElementDeserializer extends StdScalarDeserializer<StackTraceElement> {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new initialized instance.
     */
    public Log4jStackTraceElementDeserializer() {
        super(StackTraceElement.class);
    }

    @Override
    public StackTraceElement deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
        JsonToken t = jp.getCurrentToken();
        // Must get an Object
        if (t == JsonToken.START_OBJECT) {
            String className = null, methodName = null, fileName = null;
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
                        // An XML number always comes in a string since there is no syntax help as with JSON.
                        try {
                            lineNumber = Integer.parseInt(jp.getText().trim());
                        } catch (final NumberFormatException e) {
                            throw JsonMappingException.from(jp, "Non-numeric token (" + t + ") for property 'line'", e);
                        }
                    }
                } else if ("method".equals(propName)) {
                    methodName = jp.getText();
                } else if ("nativeMethod".equals(propName)) {
                    // no setter, not passed via constructor: ignore
                } else {
                    this.handleUnknownProperty(jp, ctxt, this._valueClass, propName);
                }
            }
            return new StackTraceElement(className, methodName, fileName, lineNumber);
        }
        throw ctxt.mappingException(this._valueClass, t);
    }
}