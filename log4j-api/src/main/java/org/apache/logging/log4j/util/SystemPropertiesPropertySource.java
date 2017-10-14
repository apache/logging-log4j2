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

/**
 * PropertySource backed by the current system properties. Other than having a higher priority over normal properties,
 * this follows the same rules as {@link PropertiesPropertySource}.
 *
 * @since 2.10.0
 */
public class SystemPropertiesPropertySource implements PropertySource {

    private static final String PREFIX = "log4j2.";

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void forEach(final BiConsumer<String, String> action) {
        for (final Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            action.accept(((String) entry.getKey()), ((String) entry.getValue()));
        }
    }

    @Override
    public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        return PREFIX + Util.joinAsCamelCase(tokens);
    }

}
