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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ContextData;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;

/**
 * Looks up keys from the context. By default this is the {@link ThreadContext} or {@link ScopedContext}. Users may
 * add their own {@link org.apache.logging.log4j.core.util.ContextDataProvider} which can be retrieved via this
 * Lookup.
 */
@Plugin(name = "ctx", category = StrLookup.CATEGORY)
public class ContextMapLookup implements StrLookup {

    private final ContextDataInjector injector = ContextDataInjectorFactory.createInjector();

    /**
     * Looks up the value from the ThreadContext Map.
     * @param key  the key to be looked up, may be null
     * @return The value associated with the key.
     */
    @Override
    public String lookup(final String key) {
        if (injector == null) {
            return ContextData.getValue(key);
        }
        return injector.getValue(key);
    }

    /**
     * Looks up the value from the ThreadContext Map.
     * @param event The current LogEvent.
     * @param key  the key to be looked up, may be null
     * @return The value associated with the key.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        return event == null ? null : event.getContextData().getValue(key);
    }
}
