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

package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.util.NameUtil;
import org.apache.logging.log4j.util.StringBuilders;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * PluginInjectionBuilder implementation for {@link PluginAttribute}.
 */
public class PluginAttributeBuilder extends AbstractPluginInjectionBuilder<PluginAttribute, Object> {

    private static final Map<Class<?>, Function<PluginAttribute, Object>> DEFAULT_VALUE_EXTRACTORS;

    static {
        final Map<Class<?>, Function<PluginAttribute, Object>> extractors = new ConcurrentHashMap<>();
        extractors.put(int.class, PluginAttribute::defaultInt);
        extractors.put(Integer.class, PluginAttribute::defaultInt);
        extractors.put(long.class, PluginAttribute::defaultLong);
        extractors.put(Long.class, PluginAttribute::defaultLong);
        extractors.put(boolean.class, PluginAttribute::defaultBoolean);
        extractors.put(Boolean.class, PluginAttribute::defaultBoolean);
        extractors.put(float.class, PluginAttribute::defaultFloat);
        extractors.put(Float.class, PluginAttribute::defaultFloat);
        extractors.put(double.class, PluginAttribute::defaultDouble);
        extractors.put(Double.class, PluginAttribute::defaultDouble);
        extractors.put(byte.class, PluginAttribute::defaultByte);
        extractors.put(Byte.class, PluginAttribute::defaultByte);
        extractors.put(char.class, PluginAttribute::defaultChar);
        extractors.put(Character.class, PluginAttribute::defaultChar);
        extractors.put(short.class, PluginAttribute::defaultShort);
        extractors.put(Short.class, PluginAttribute::defaultShort);
        extractors.put(Class.class, PluginAttribute::defaultClass);
        DEFAULT_VALUE_EXTRACTORS = Collections.unmodifiableMap(extractors);
    }

    public PluginAttributeBuilder() {
        super(PluginAttribute.class);
    }

    @Override
    public Object build() {
        final String name = this.annotation.value();
        final Map<String, String> attributes = node.getAttributes();
        final String rawValue = removeAttributeValue(attributes, name, this.aliases);
        final String replacedValue = stringSubstitutionStrategy.apply(rawValue);
        final Object defaultValue = findDefaultValue();
        final Object value = convert(replacedValue, defaultValue);
        final Object debugValue = this.annotation.sensitive() ? NameUtil.md5(value + this.getClass().getName()) : value;
        StringBuilders.appendKeyDqValue(debugLog, name, debugValue);
        return value;
    }

    private Object findDefaultValue() {
        return DEFAULT_VALUE_EXTRACTORS.getOrDefault(
                conversionType,
                pluginAttribute -> stringSubstitutionStrategy.apply(pluginAttribute.defaultString())
        ).apply(annotation);
    }
}
