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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.util.Constants;

/**
 * Able to append MDC info to each line of the stacktrace of the thrown throwable object in
 * {@link LogEvent}.
 */
@Plugin(name = "MdcThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"cEx", "cThrowable", "cException"})
public class MdcThrowablePatternConverter extends ThrowablePatternConverter {

    private static final String CAUSED_BY = "Caused By: ";

    private final List<String> mdcKeys;

    /**
     * Obtain an instance of this ThrowablePatternConverter.
     *
     * @see PatternParser#createConverter(String, StringBuilder, Map, List, boolean)
     */
    public static MdcThrowablePatternConverter newInstance(String[] options) {
        return new MdcThrowablePatternConverter(options);
    }


    /**
     * Constructor.
     *
     * @param options options, may be null.
     */
    private MdcThrowablePatternConverter(final String[] options) {
        super("MdcThrowable", "throwable", null);
        if (options == null || options.length == 0) {
            this.mdcKeys = Collections.emptyList();
        } else {
            String option = options[0];
            String[] keys = option.split("\\s*,\\s*");
            Arrays.sort(keys);
            this.mdcKeys = Arrays.asList(keys);
        }
    }

    @Override
    public void format(final LogEvent event, final StringBuilder buffer) {
        ThrowableProxy throwable = event.getThrownProxy();

        String suffix = assembleSuffix(event.getContextMap());

        while (throwable != null) {
            formatThrowable(throwable, buffer, suffix);
            ThrowableProxy causeProxy = throwable.getCauseProxy();
            if (causeProxy != null && !causeProxy.equals(throwable)) {
                throwable = causeProxy;
                buffer.append(CAUSED_BY);
            } else {
                throwable = null;
            }
        }
    }

    private String assembleSuffix(final Map<String, String> contextMap) {
        if (mdcKeys.isEmpty()) {
            List<String> mdcKeys = new ArrayList<>(contextMap.keySet());
            Collections.sort(mdcKeys);
            return assembleMdcInfoString(contextMap, mdcKeys);
        } else if (mdcKeys.size() == 1) {
            String value = contextMap.get(mdcKeys.get(0));
            return value == null ? "" : value;
        } else {
            return assembleMdcInfoString(contextMap, mdcKeys);
        }
    }

    private String assembleMdcInfoString(final Map<String, String> contextMap, final Iterable<String> mdcKeys) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (String key : mdcKeys) {
            if (stringBuilder.length() > 1) {
                stringBuilder.append(", ");
            }
            String value = contextMap.get(key);
            stringBuilder.append(key).append("=").append(value);
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private void formatThrowable(final ThrowableProxy throwable, final StringBuilder buffer, final String suffix) {
        ExtendedStackTraceElement[] extendedStackTrace = throwable.getExtendedStackTrace();
        int count = throwable.getCommonElementCount();

        buffer.append(throwable.getThrowable()).append(" ").append(suffix).append(Constants.LINE_SEPARATOR);

        for (int i = 0; i < extendedStackTrace.length - count; i++) {
            buffer.append("\tat ").append(extendedStackTrace[i]).append(" ").append(suffix).append(Constants.LINE_SEPARATOR);
        }

        if (count > 0) {
            buffer.append("\t").append("... ").append(count).append(" more").append(" ").append(suffix).append(Constants.LINE_SEPARATOR);
        }
    }
}
