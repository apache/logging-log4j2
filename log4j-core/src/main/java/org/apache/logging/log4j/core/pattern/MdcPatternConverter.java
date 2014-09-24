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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Able to handle the contents of the LogEvent's MDC and either
 * output the entire contents of the properties in a similar format to the
 * java.util.Hashtable.toString(), or to output the value of a specific key
 * within the property bundle
 * when this pattern converter has the option set.
 */
 @Plugin(name = "MdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "X", "mdc", "MDC" })
public final class MdcPatternConverter extends LogEventPatternConverter {
    /**
     * Name of property to output.
     */
    private final String key;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private MdcPatternConverter(final String[] options) {
        super(options != null && options.length > 0 ? "MDC{" + options[0] + '}' : "MDC", "mdc");
        key = options != null && options.length > 0 ? options[0] : null;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final Map<String, String> contextMap = event.getContextMap();
        // if there is no additional options, we output every single
        // Key/Value pair for the MDC in a similar format to Hashtable.toString()
        if (key == null) {


            if (contextMap == null || contextMap.isEmpty()) {
                toAppendTo.append("{}");
                return;
            }
            final StringBuilder sb = new StringBuilder("{");
            final Set<String> keys = new TreeSet<String>(contextMap.keySet());
            for (final String key : keys) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(key).append('=').append(contextMap.get(key));

            }
            sb.append('}');
            toAppendTo.append(sb);
        } else if (contextMap != null) {
            // otherwise they just want a single key output
            final Object val = contextMap.get(key);

            if (val != null) {
                toAppendTo.append(val);
            }
        }
    }
}
