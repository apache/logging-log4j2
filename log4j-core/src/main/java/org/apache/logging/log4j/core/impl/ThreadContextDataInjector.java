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
package org.apache.logging.log4j.core.impl;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextData;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.spi.ThreadContextMap;

/**
 * {@code ThreadContextDataInjector} copies key-value pairs from the {@code ThreadContext} map and from the
 * configuration {@code Properties} into the {@code LogEvent}s' {@code ContextData}.
 * <p>
 * This is the default {@code ContextDataInjector} returned by the {@link ContextDataInjectorFactory}.
 * </p>
 *
 * @see org.apache.logging.log4j.ThreadContext
 * @see Property
 * @see ContextData
 * @see ContextDataInjector
 * @see ContextDataInjectorFactory
 * @since 2.7
 */
public class ThreadContextDataInjector implements ContextDataInjector {
    @Override
    public void injectContextData(final List<Property> properties, final MutableContextData contextData) {
        copyProperties(properties, contextData);
        //copyThreadContextMap(ThreadContext.getThreadContextMap(), contextData); //TODO LOG4J2-1349
        copyThreadContextMap(ThreadContext.getImmutableContext(), contextData);
    }

    private void copyProperties(final List<Property> properties, final MutableContextData contextData) {
        if (properties != null) {
            for (int i = 0; i < properties.size(); i++) {
                final Property prop = properties.get(i);
                contextData.putValue(prop.getName(), prop.getValue());
            }
        }
    }

    private void copyThreadContextMap(final Map<String, String> map, final MutableContextData contextData) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                contextData.putValue(entry.getKey(), entry.getValue());
            }
        }
    }

    private void copyThreadContextMap(final ThreadContextMap contextMap, final MutableContextData contextData) {
        if (contextMap instanceof ContextData) {
            contextData.putAll((ContextData) contextMap);
        } else {
            if (contextMap != null) {
                copyThreadContextMap(contextMap.getImmutableMapOrNull(), contextData);
            }
        }
    }
}
