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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.status.StatusLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts an Object to a JSON String.
 */
public class JsonMessage implements Message {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Object object;

    /**
     * Constructs a JsonMessage.
     *
     * @param object the Object to serialize.
     */
    public JsonMessage(final Object object) {
        this.object = object;
    }

    @Override
    public String getFormattedMessage() {
        try {
            return mapper.writeValueAsString(object);
        } catch (final JsonProcessingException e) {
            StatusLogger.getLogger().catching(e);
            return object.toString();
        }
    }

    @Override
    public String getFormat() {
        return object.toString();
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {object};
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }
}
