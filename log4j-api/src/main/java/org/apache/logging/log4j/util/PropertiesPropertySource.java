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

import java.util.Map;
import java.util.Properties;

/**
 * PropertySource backed by a {@link Properties} instance. Normalized property names follow a scheme like this:
 * {@code Log4jContextSelector} would normalize to {@code log4j2.contextSelector}.
 *
 * @since 2.10.0
 */
public class PropertiesPropertySource implements PropertySource {

    private static final String PREFIX = "log4j2.";

    private final Properties properties;

    public PropertiesPropertySource(final Properties properties) {
        this.properties = properties;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void forEach(final BiConsumer<String, String> action) {
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            action.accept(((String) entry.getKey()), ((String) entry.getValue()));
        }
    }

    @Override
    public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        return PREFIX + Util.joinAsCamelCase(tokens);
    }
}
