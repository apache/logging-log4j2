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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.layout.template.json.util.RecyclerFactories;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactory;
import org.apache.logging.log4j.util.PropertiesUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.TimeZone;

public enum JsonTemplateLayoutDefaults {;

    private static final PropertiesUtil PROPERTIES = PropertiesUtil.getProperties();

    private static final Charset CHARSET = readCharset();

    private static final boolean LOCATION_INFO_ENABLED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.locationInfoEnabled",
                    false);

    private static final boolean STACK_TRACE_ENABLED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.stackTraceEnabled",
                    true);

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
                    "log4j.layout.jsonTemplate.eventTemplateUri",
                    "classpath:EcsLayout.json");

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

    private static final boolean NULL_EVENT_DELIMITER_ENABLED =
            PROPERTIES.getBooleanProperty(
                    "log4j.layout.jsonTemplate.nullEventDelimiterEnabled",
                    false);

    private static final int MAX_STRING_LENGTH = readMaxStringLength();

    private static final String TRUNCATED_STRING_SUFFIX =
            PROPERTIES.getStringProperty(
                    "log4j.layout.jsonTemplate.truncatedStringSuffix",
                    "…");

    private static final RecyclerFactory RECYCLER_FACTORY = readRecyclerFactory();

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

    private static int readMaxStringLength() {
        final int maxStringLength = PROPERTIES.getIntegerProperty(
                "log4j.layout.jsonTemplate.maxStringLength",
                16 * 1_024);
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException(
                    "was expecting a non-zero positive maxStringLength: " +
                            maxStringLength);
        }
        return maxStringLength;
    }

    private static RecyclerFactory readRecyclerFactory() {
        final String recyclerFactorySpec = PROPERTIES.getStringProperty(
                "log4j.layout.jsonTemplate.recyclerFactory");
        return RecyclerFactories.ofSpec(recyclerFactorySpec);
    }

    public static Charset getCharset() {
        return CHARSET;
    }

    public static boolean isLocationInfoEnabled() {
        return LOCATION_INFO_ENABLED;
    }

    public static boolean isStackTraceEnabled() {
        return STACK_TRACE_ENABLED;
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

    public static boolean isNullEventDelimiterEnabled() {
        return NULL_EVENT_DELIMITER_ENABLED;
    }

    public static int getMaxStringLength() {
        return MAX_STRING_LENGTH;
    }

    public static String getTruncatedStringSuffix() {
        return TRUNCATED_STRING_SUFFIX;
    }

    public static RecyclerFactory getRecyclerFactory() {
        return RECYCLER_FACTORY;
    }

}
