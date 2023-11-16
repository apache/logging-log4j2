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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.io.IOException;
import org.apache.logging.log4j.core.util.Integers;

/**
 * Used by Jackson to deserialize a {@link StackTraceElement}. Serialization is
 * performed by {@link StackTraceElementMixIn}.
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
    public StackTraceElement deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
        JsonToken t = jp.getCurrentToken();
        // Must get an Object
        if (t == JsonToken.START_OBJECT) {
            String className = null, methodName = null, fileName = null;
            int lineNumber = -1;

            while ((t = jp.nextValue()) != JsonToken.END_OBJECT) {
                final String propName = jp.getCurrentName();
                switch (propName) {
                    case StackTraceElementConstants.ATTR_CLASS: {
                        className = jp.getText();
                        break;
                    }
                    case StackTraceElementConstants.ATTR_FILE: {
                        fileName = jp.getText();
                        break;
                    }
                    case StackTraceElementConstants.ATTR_LINE: {
                        if (t.isNumeric()) {
                            lineNumber = jp.getIntValue();
                        } else {
                            // An XML number always comes in a string since there is no syntax help as with JSON.
                            try {
                                lineNumber = Integers.parseInt(jp.getText());
                            } catch (final NumberFormatException e) {
                                throw JsonMappingException.from(
                                        jp, "Non-numeric token (" + t + ") for property 'line'", e);
                            }
                        }
                        break;
                    }
                    case StackTraceElementConstants.ATTR_METHOD: {
                        methodName = jp.getText();
                        break;
                    }
                    case "nativeMethod": {
                        // no setter, not passed via constructor: ignore
                    }
                    default: {
                        this.handleUnknownProperty(jp, ctxt, this._valueClass, propName);
                    }
                }
            }
            return new StackTraceElement(className, methodName, fileName, lineNumber);
        }
        throw JsonMappingException.from(
                jp,
                String.format(
                        "Cannot deserialize instance of %s out of %s token", ClassUtil.nameOf(this._valueClass), t));
    }
}
