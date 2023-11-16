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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.util.StringMap;

/**
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class ContextDataAsEntryListDeserializer extends StdDeserializer<StringMap> {

    private static final long serialVersionUID = 1L;

    ContextDataAsEntryListDeserializer() {
        super(Map.class);
    }

    @Override
    public StringMap deserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        final List<MapEntry> list = jp.readValueAs(
                new TypeReference<List<MapEntry>>() {
                    // empty
                });
        final StringMap contextData = new ContextDataFactory().createContextData();
        for (final MapEntry mapEntry : list) {
            contextData.putValue(mapEntry.getKey(), mapEntry.getValue());
        }
        return contextData;
    }
}
