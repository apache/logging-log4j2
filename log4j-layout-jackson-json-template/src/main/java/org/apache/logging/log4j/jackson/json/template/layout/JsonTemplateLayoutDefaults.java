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
package org.apache.logging.log4j.jackson.json.template.layout;

import org.apache.logging.log4j.util.PropertiesUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.TimeZone;

public enum JsonTemplateLayoutDefaults {;

    private static final PropertiesUtil PROPERTIES = PropertiesUtil.getProperties();

    private static final Charset CHARSET = readCharset();

    private static final boolean PRETTY_PRINT_ENABLED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.prettyPrintEnabled",
                    false);

    private static final boolean LOCATION_INFO_ENABLED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.locationInfoEnabled",
                    false);

    private static final boolean STACK_TRACE_ENABLED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.stackTraceEnabled",
                    true);

    private static final boolean BLANK_FIELD_EXCLUSION_ENABLED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.blankFieldExclusionEnabled",
                    false);

    private static final String TIMESTAMP_FORMAT_PATTERN =
            PROPERTIES.getStringProperty(
                    "log4j.layout.jsonTemplate.timestampFormatPattern",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ");

    private static final TimeZone TIME_ZONE = readTimeZone();

    private static final Locale LOCALE = readLocale();

    private static final String EVENT_TEMPLATE =
            PROPERTIES.getStringProperty(
                    "log4j.layout.jsonTemplate.eventTemplate");

    private static final String EVENT_TEMPLATE_URI =
            PROPERTIES.getStringProperty(
                    "log4j.layout.jsonTemplate.eventElementTemplateUri",
                    "classpath:LogstashJsonEventLayoutV1.json");

    private static final String STACK_TRACE_ELEMENT_TEMPLATE =
            PROPERTIES.getStringProperty(
                    "log4j.layout.jsonTemplate.stackTraceElementTemplate");

    private static final String STACK_TRACE_ELEMENT_TEMPLATE_URI =
            PROPERTIES.getStringProperty(
                    "log4j.layout.jsonTemplate.stackTraceElementTemplateUri",
                    "classpath:StackTraceElementLayout.json");

    private static final String MDC_KEY_PATTERN =
            PROPERTIES.getStringProperty("log4j.layout.jsonTemplate.mdcKeyPattern");

    private static final String NDC_PATTERN =
            PROPERTIES.getStringProperty("log4j.layout.jsonTemplate.ndcPattern");

    private static final String EVENT_DELIMITER =
            PROPERTIES.getStringProperty(
                    "log4j.layout.jsonTemplate.eventDelimiter",
                    System.lineSeparator());

    private static final int MAX_BYTE_COUNT =
            PROPERTIES.getIntegerProperty(
                    "log4j.layout.jsonTemplate.maxByteCount",
                    512 * 1024);    // 512 KiB

    private static final int MAX_STRING_LENGTH =
            PROPERTIES.getIntegerProperty(
                    "log4j.layout.jsonTemplate.maxStringLength",
                    0);

    private static final String OBJECT_MAPPER_FACTORY_METHOD =
            "com.fasterxml.jackson.databind.ObjectMapper.new";

    private static final boolean MAP_MESSAGE_FORMATTER_IGNORED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.mapMessageFormatterIgnored",
                    true);

    private static Charset readCharset() {
        final String charsetName =
                PROPERTIES.getStringProperty("log4j.layout.jsonTemplate.charset");
        return charsetName != null
                ? Charset.forName(charsetName)
                : StandardCharsets.UTF_8;
    }

    private static TimeZone readTimeZone() {
        final String timeZoneId =
                PROPERTIES.getStringProperty("log4j.layout.jsonTemplate.timeZone");
        return timeZoneId != null
                ? TimeZone.getTimeZone(timeZoneId)
                : TimeZone.getDefault();
    }

    private static Locale readLocale() {
        final String locale =
                PROPERTIES.getStringProperty("log4j.layout.jsonTemplate.locale");
        if (locale == null) {
            return Locale.getDefault();
        }
        final String[] localeFields = locale.split("_", 3);
        switch (localeFields.length) {
            case 1: return new Locale(localeFields[0]);
            case 2: return new Locale(localeFields[0], localeFields[1]);
            case 3: return new Locale(localeFields[0], localeFields[1], localeFields[2]);
            default: throw new IllegalArgumentException("invalid locale: " + locale);
        }
    }

    public static Charset getCharset() {
        return CHARSET;
    }

    public static boolean isPrettyPrintEnabled() {
        return PRETTY_PRINT_ENABLED;
    }

    public static boolean isLocationInfoEnabled() {
        return LOCATION_INFO_ENABLED;
    }

    public static boolean isStackTraceEnabled() {
        return STACK_TRACE_ENABLED;
    }

    public static boolean isBlankFieldExclusionEnabled() {
        return BLANK_FIELD_EXCLUSION_ENABLED;
    }

    public static String getTimestampFormatPattern() {
        return TIMESTAMP_FORMAT_PATTERN;
    }

    public static TimeZone getTimeZone() {
        return TIME_ZONE;
    }

    public static Locale getLocale() {
        return LOCALE;
    }

    public static String getEventTemplate() {
        return EVENT_TEMPLATE;
    }

    public static String getEventTemplateUri() {
        return EVENT_TEMPLATE_URI;
    }

    public static String getStackTraceElementTemplate() {
        return STACK_TRACE_ELEMENT_TEMPLATE;
    }

    public static String getStackTraceElementTemplateUri() {
        return STACK_TRACE_ELEMENT_TEMPLATE_URI;
    }

    public static String getMdcKeyPattern() {
        return MDC_KEY_PATTERN;
    }

    public static String getNdcPattern() {
        return NDC_PATTERN;
    }

    public static String getEventDelimiter() {
        return EVENT_DELIMITER;
    }

    public static int getMaxByteCount() {
        return MAX_BYTE_COUNT;
    }

    public static int getMaxStringLength() {
        return MAX_STRING_LENGTH;
    }

    public static String getObjectMapperFactoryMethod() {
        return OBJECT_MAPPER_FACTORY_METHOD;
    }

    public static boolean isMapMessageFormatterIgnored() {
        return MAP_MESSAGE_FORMATTER_IGNORED;
    }

}
