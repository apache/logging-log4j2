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
package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * The default {@link LogEventPatternConverter} for handling {@link Throwable}s, if none is provided by the user.
 */
final class DefaultThrowablePatternConverter extends LogEventPatternConverter {

    private final LogEventPatternConverter delegateConverter;

    DefaultThrowablePatternConverter(final Configuration config) {
        super(null, null);
        this.delegateConverter = ExtendedThrowablePatternConverter.newInstance(config, new String[0]);
    }

    @Override
    public void format(final LogEvent event, StringBuilder buffer) {
        if (event.getThrown() != null) {
            ensureNewlinePrefix(buffer);
            delegateConverter.format(event, buffer);
        }
    }

    private static void ensureNewlinePrefix(final StringBuilder buffer) {
        final int bufferLength = buffer.length();
        if (bufferLength > 0 && buffer.charAt(bufferLength - 1) != '\n') {
            buffer.append(System.lineSeparator());
        }
    }

    /**
     * Indicates this converter handles {@link Throwable}s.
     *
     * @return {@code true}
     */
    @Override
    public boolean handlesThrowable() {
        return true;
    }
}
