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
package org.apache.logging.log4j.util;

import java.util.Properties;

/**
 * PropertySource implementation that uses environment variables as a source.
 * All environment variables must begin with {@code LOG4J_} so as not to
 * conflict with other variables. Normalized environment variables follow a
 * scheme like this: {@code log4j2.fooBarProperty} would normalize to
 * {@code LOG4J_FOO_BAR_PROPERTY}.
 *
 * @since 2.10.0
 */
public class EnvironmentPropertySource extends ContextAwarePropertySource implements ReloadablePropertySource {
    private static final int DEFAULT_PRIORITY = 100;

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    public EnvironmentPropertySource() {
        super(System.getenv());
    }

    @Override
    public void forEach(final BiConsumer<String, String> action) {
        final Properties properties = propertiesMap.get(SYSTEM_CONTEXT);
        if (properties == null) {
            return;
        }
        for (final String key : properties.stringPropertyNames()) {
            action.accept(key, properties.getProperty(key));
        }
    }

    @Override
    public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        final StringBuilder sb = new StringBuilder();
        for (final CharSequence token : tokens) {
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(token);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public void reload() {
        propertiesMap.putAll(parseProperties(System.getenv()));
    }
}
