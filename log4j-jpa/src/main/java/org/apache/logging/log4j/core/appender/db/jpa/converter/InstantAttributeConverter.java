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
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * A JPA 2.1 attribute converter for {@link Instant}s in {@link org.apache.logging.log4j.core.LogEvent}s. This
 * converter is capable of converting both to and from {@link String}s.
 */
@Converter(autoApply = false)
public class InstantAttributeConverter implements AttributeConverter<Instant, String> {
    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    @Override
    public String convertToDatabaseColumn(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.getEpochSecond() + "," + instant.getNanoOfSecond();
    }

    @Override
    public Instant convertToEntityAttribute(final String s) {
        if (Strings.isEmpty(s)) {
            return null;
        }

        final int pos = s.indexOf(",");
        final long epochSecond = Long.parseLong(s.substring(0, pos));
        final int nanos = Integer.parseInt(s.substring(pos + 1, s.length()));

        final MutableInstant result = new MutableInstant();
        result.initFromEpochSecond(epochSecond, nanos);
        return result;
    }
}
