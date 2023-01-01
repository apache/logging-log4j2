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

import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Factory for creating the StringMap instances used to initialize LogEvents' {@linkplain LogEvent#getContextData()
 * context data}. When context data is {@linkplain ContextDataInjector injected} into the log event, these StringMap
 * instances may be either populated with key-value pairs from the context, or completely replaced altogether.
 * <p>
 * By default returns {@code SortedArrayStringMap} objects. Can be configured by registering a custom binding for
 * {@link ContextDataFactory}.
 * </p>
 *
 * @see LogEvent#getContextData()
 * @see ContextDataInjector
 * @see SortedArrayStringMap
 * @since 3.0.0
 */
public class DefaultContextDataFactory implements ContextDataFactory {
    private final SortedArrayStringMap emptyMap = new SortedArrayStringMap(0);

    public DefaultContextDataFactory() {
        emptyMap.freeze();
    }

    @Override
    public StringMap createContextData() {
        return new SortedArrayStringMap();
    }

    @Override
    public StringMap createContextData(final int initialCapacity) {
        return new SortedArrayStringMap(initialCapacity);
    }

    @Override
    public StringMap emptyFrozenContextData() {
        return emptyMap;
    }
}
