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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Map;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class ContextDataSerializer extends StdSerializer<ReadOnlyStringMap> {

    private static final long serialVersionUID = 1L;

    protected ContextDataSerializer() {
        super(Map.class, false);
    }

    @Override
    public void serialize(
            final ReadOnlyStringMap contextData, final JsonGenerator jgen, final SerializerProvider provider)
            throws IOException, JsonGenerationException {

        jgen.writeStartObject();
        contextData.forEach(WRITE_STRING_FIELD_INTO, jgen);
        jgen.writeEndObject();
    }

    private static final TriConsumer<String, Object, JsonGenerator> WRITE_STRING_FIELD_INTO =
            (key, value, jsonGenerator) -> {
                try {
                    if (value == null) {
                        jsonGenerator.writeNullField(key);
                    } else {
                        jsonGenerator.writeStringField(key, String.valueOf(value));
                    }
                } catch (final Exception ex) {
                    throw new IllegalStateException("Problem with key " + key, ex);
                }
            };
}
