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

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.util.Strings;

/**
 * A JPA 2.1 attribute converter for {@link Marker}s in {@link org.apache.logging.log4j.core.LogEvent}s. This
 * converter is capable of converting both to and from {@link String}s.
 */
@Converter(autoApply = false)
public class MarkerAttributeConverter implements AttributeConverter<Marker, String> {
    @Override
    public String convertToDatabaseColumn(final Marker marker) {
        if (marker == null) {
            return null;
        }
        return marker.toString();
    }

    @Override
    public Marker convertToEntityAttribute(final String s) {
        if (Strings.isEmpty(s)) {
            return null;
        }

        final int bracket = s.indexOf("[");

        return bracket < 1 ? MarkerManager.getMarker(s) : MarkerManager.getMarker(s.substring(0, bracket));
    }
}
