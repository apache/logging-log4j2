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
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PluginAttributeInjector extends AbstractConfigurationInjector<PluginAttribute, Object> {

    private static final Map<Type, Function<PluginAttribute, Object>> DEFAULT_VALUE_EXTRACTORS;

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

    @Override
    public void inject(final Object factory) {
        final Optional<String> value = findAndRemoveNodeAttribute().map(stringSubstitutionStrategy);
        if (value.isPresent()) {
            configurationBinder.bindString(factory, value.get());
        } else {
            injectDefaultValue(factory);
        }
    }

    private void injectDefaultValue(final Object factory) {
        final Function<PluginAttribute, Object> extractor = DEFAULT_VALUE_EXTRACTORS.get(conversionType);
        if (extractor != null) {
            final Object value = extractor.apply(annotation);
            debugLog(value);
            configurationBinder.bindObject(factory, value);
        } else {
            final String value = stringSubstitutionStrategy.apply(annotation.defaultString());
            if (Strings.isNotBlank(value)) {
                debugLog(value);
                configurationBinder.bindString(factory, value);
            }
        }
    }

    private void debugLog(final Object value) {
        final Object debugValue = annotation.sensitive() ? "*****" : value;
        StringBuilders.appendKeyDqValue(debugLog, name, debugValue);
    }

}
