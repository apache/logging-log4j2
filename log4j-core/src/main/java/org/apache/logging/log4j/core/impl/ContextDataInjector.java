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

import org.apache.logging.log4j.core.ContextData;
import org.apache.logging.log4j.core.config.Property;

/**
 * Responsible for initializing the ContextData of LogEvents. Context data is data that is set by the application to be
 * included in all subsequent log events.
 * <p>
 * The source of the context data is implementation-specific. The default source for context data is the ThreadContext.
 * </p><p>
 * In some asynchronous models, work may be delegated to several threads, while conceptually this work shares the same
 * context. In such models, storing context data in {@code ThreadLocal} variables is not convenient or desirable.
 * Users can configure the {@code ContextDataInjectorFactory} to provide custom {@code ContextDataInjector} objects,
 * in order to initialize log events with context data from any arbitrary context.
 * </p><p>
 * When providing a custom {@code ContextDataInjector}, be aware that the {@code ContextDataFactory} may be invoked
 * multiple times by the various components in Log4j that need access to context data.
 * This includes the object(s) that populate log events, but also various lookups and filters that look at
 * context data to determine whether an event should be logged.
 * </p>
 *
 * @see ContextDataInjectorFactory
 * @see org.apache.logging.log4j.core.ContextData
 * @see org.apache.logging.log4j.ThreadContext
 * @see ThreadContextDataInjector
 * @since 2.7
 */
public interface ContextDataInjector {
    /**
     * Returns a {@code MutableContextData} object initialized with the specified properties and the appropriate
     * context data. The returned value may be the specified parameter or a different object.
     * <p>
     * Thread-safety note: The returned object can safely be passed off to another thread: future changes in the
     * underlying context data will not be reflected in the returned object.
     * </p>
     *
     * @param properties Properties from the log4j configuration to be added to the resulting ContextData. May be
     *          {@code null} or empty
     * @param reusable a {@code MutableContextData} instance that may be reused to avoid creating temporary objects
     * @return a {@code MutableContextData} instance initialized with the specified properties and the appropriate
     *          context data. The returned value may be the specified parameter or a different object.
     */
    MutableContextData injectContextData(final List<Property> properties, final MutableContextData reusable);

    /**
     * Returns a {@code ContextData} object reflecting the current state of the context.
     * <p>
     * Thread-safety note: The returned object can only be safely used <em>in the current thread</em>. Changes in the
     * underlying context may or may not be reflected in the returned object, depending on the context data source and
     * the implementation of this method. It is not safe to pass the returned object to another thread.
     * </p>
     * @return a {@code ContextData} object reflecting the current state of the context
     */
    ContextData rawContextData();
}
