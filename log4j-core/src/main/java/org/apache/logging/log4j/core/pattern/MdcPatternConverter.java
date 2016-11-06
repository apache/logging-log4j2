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
package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.TriConsumer;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Able to handle the contents of the LogEvent's MDC and either
 * output the entire contents of the properties in a similar format to the
 * java.util.Hashtable.toString(), or to output the value of a specific key
 * within the property bundle
 * when this pattern converter has the option set.
 */
@Plugin(name = "MdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "X", "mdc", "MDC" })
@PerformanceSensitive("allocation")
public final class MdcPatternConverter extends LogEventPatternConverter {

    private static final ThreadLocal<StringBuilder> threadLocal = new ThreadLocal<>();
    private static final int DEFAULT_STRING_BUILDER_SIZE = 64;
    private static final int MAX_STRING_BUILDER_SIZE = Constants.MAX_REUSABLE_MESSAGE_SIZE;

    /**
     * Name of property to output.
     */
    private final String key;
    private final String[] keys;
    private final boolean full;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private MdcPatternConverter(final String[] options) {
        super(options != null && options.length > 0 ? "MDC{" + options[0] + '}' : "MDC", "mdc");
        if (options != null && options.length > 0) {
            full = false;
            if (options[0].indexOf(',') > 0) {
                keys = options[0].split(",");
                for (int i = 0; i < keys.length; i++) {
                    keys[i] = keys[i].trim();
                }
                key = null;
            } else {
                keys = null;
                key = options[0];
            }
        } else {
            full = true;
            key = null;
            keys = null;
        }
    }

    /**
     * Obtains an instance of PropertiesPatternConverter.
     *
     * @param options options, may be null or first element contains name of property to format.
     * @return instance of PropertiesPatternConverter.
     */
    public static MdcPatternConverter newInstance(final String[] options) {
        return new MdcPatternConverter(options);
    }

    private static final TriConsumer<String, Object, StringBuilder> WRITE_KEY_VALUES_INTO = new TriConsumer<String, Object, StringBuilder>() {
        @Override
        public void accept(final String key, final Object value, final StringBuilder sb) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(key).append('=');
            StringBuilders.appendValue(sb, value);
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final ReadOnlyStringMap contextData = event.getContextData();
        // if there is no additional options, we output every single
        // Key/Value pair for the MDC in a similar format to Hashtable.toString()
        if (full) {
            if (contextData == null || contextData.size() == 0) {
                toAppendTo.append("{}");
                return;
            }
            appendFully(contextData, toAppendTo);
        } else {
            if (keys != null) {
                if (contextData == null || contextData.size() == 0) {
                    toAppendTo.append("{}");
                    return;
                }
                appendSelectedKeys(keys, contextData, toAppendTo);
            } else if (contextData != null){
                // otherwise they just want a single key output
                final Object value = contextData.getValue(key);
                if (value != null) {
                    StringBuilders.appendValue(toAppendTo, value);
                }
            }
        }
    }

    private static void appendFully(final ReadOnlyStringMap contextData, final StringBuilder toAppendTo) {
        final StringBuilder sb = getStringBuilder();
        sb.append("{");
        contextData.forEach(WRITE_KEY_VALUES_INTO, sb);
        sb.append('}');
        toAppendTo.append(sb);
        trimToMaxSize(sb);
    }

    private static void appendSelectedKeys(final String[] keys, final ReadOnlyStringMap contextData, final StringBuilder toAppendTo) {
        // Print all the keys in the array that have a value.
        final StringBuilder sb = getStringBuilder();
        sb.append("{");
        for (int i = 0; i < keys.length; i++) {
            final String theKey = keys[i];
            final Object value = contextData.getValue(theKey);
            if (value != null) { // !contextData.containskey(theKey)
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(theKey).append('=');
                StringBuilders.appendValue(sb, value);
            }
        }
        sb.append('}');
        toAppendTo.append(sb);
        trimToMaxSize(sb);
    }

    private static StringBuilder getStringBuilder() {
        StringBuilder result = threadLocal.get();
        if (result == null) {
            result = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            threadLocal.set(result);
        }
        result.setLength(0);
        return result;
    }

    private static void trimToMaxSize(final StringBuilder stringBuilder) {
        if (stringBuilder.length() > MAX_STRING_BUILDER_SIZE) {
            stringBuilder.setLength(MAX_STRING_BUILDER_SIZE);
            stringBuilder.trimToSize();
        }
    }
}
