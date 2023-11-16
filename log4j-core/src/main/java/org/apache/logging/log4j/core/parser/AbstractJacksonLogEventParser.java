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
package org.apache.logging.log4j.core.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

class AbstractJacksonLogEventParser implements TextLogEventParser {
    private final ObjectReader objectReader;

    AbstractJacksonLogEventParser(final ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectReader = objectMapper.readerFor(Log4jLogEvent.class);
    }

    @Override
    public LogEvent parseFrom(final String input) throws ParseException {
        try {
            return objectReader.readValue(input);
        } catch (final IOException e) {
            throw new ParseException(e);
        }
    }

    @Override
    public LogEvent parseFrom(final byte[] input) throws ParseException {
        try {
            return objectReader.readValue(input);
        } catch (final IOException e) {
            throw new ParseException(e);
        }
    }

    @Override
    public LogEvent parseFrom(final byte[] input, final int offset, final int length) throws ParseException {
        try {
            return objectReader.readValue(input, offset, length);
        } catch (final IOException e) {
            throw new ParseException(e);
        }
    }
}
