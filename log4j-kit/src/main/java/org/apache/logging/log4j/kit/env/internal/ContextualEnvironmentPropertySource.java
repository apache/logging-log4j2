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
package org.apache.logging.log4j.kit.env.internal;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.kit.env.PropertySource;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.Nullable;

/**
 * PropertySource backed by the current environment variables.
 * <p>
 *     Should haves a slightly lower priority than global environment variables.
 * </p>
 */
public class ContextualEnvironmentPropertySource implements PropertySource {

    private static final Pattern PROPERTY_TOKENIZER = Pattern.compile("([A-Z]?[a-z0-9]+|[A-Z0-9]+)\\.?");
    private static final int DEFAULT_PRIORITY = 0;

    private final String prefix;
    private final int priority;

    public ContextualEnvironmentPropertySource(final String contextName) {
        this(contextName, DEFAULT_PRIORITY);
    }

    public ContextualEnvironmentPropertySource(final String contextName, final int priority) {
        this.prefix = "LOG4J_CONTEXTS_" + contextName.toUpperCase(Locale.ROOT);
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public @Nullable String getProperty(final String key) {
        final String actualKey = getNormalForm(key);
        try {
            return System.getenv(actualKey);
        } catch (final SecurityException e) {
            StatusLogger.getLogger()
                    .warn(
                            "{} lacks permissions to access system property {}.",
                            getClass().getName(),
                            actualKey,
                            e);
        }
        return null;
    }

    private String getNormalForm(final CharSequence key) {
        final StringBuilder sb = new StringBuilder(prefix);
        final Matcher matcher = PROPERTY_TOKENIZER.matcher(key);
        int start = 0;
        while (matcher.find(start)) {
            start = matcher.end();
            sb.append('_').append(matcher.group(1).toUpperCase(Locale.ROOT));
        }
        return sb.toString();
    }
}
