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
package org.apache.logging.log4j.core;

import java.util.List;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.core.impl.ThreadContextDataInjector;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Responsible for initializing the context data of LogEvents. Context data is data that is set by the application to be
 * included in all subsequent log events.
 * <p><b>NOTE: It is no longer recommended that custom implementations of this interface be provided as it is
 * difficult to do. Instead, provide a custom ContextDataProvider.</b></p>
 * <p>
 * <p>
 * The source of the context data is implementation-specific. The default source for context data is the ThreadContext.
 * </p><p>
 * In some asynchronous models, work may be delegated to several threads, while conceptually this work shares the same
 * context. In such models, storing context data in {@code ThreadLocal} variables is not convenient or desirable.
 * Users can configure the {@code ContextDataInjectorFactory} to provide custom {@code ContextDataInjector} objects,
 * in order to initialize log events with context data from any arbitrary context.
 * </p><p>
 * When providing a custom {@code ContextDataInjector}, be aware that the {@code ContextDataInjectorFactory} may be
 * invoked multiple times and the various components in Log4j that need access to context data may each have their own
 * instance of {@code ContextDataInjector}.
 * This includes the object(s) that populate log events, but also various lookups and filters that look at
 * context data to determine whether an event should be logged.
 * </p><p>
 * Implementors should take particular note of how the different methods in the interface have different thread-safety
 * guarantees to enable optimal performance.
 * </p>
 *
 * @see StringMap
 * @see ReadOnlyStringMap
 * @see ContextDataInjectorFactory
 * @see org.apache.logging.log4j.ThreadContext
 * @see ThreadContextDataInjector
 * @since 2.7
 */
public interface ContextDataInjector {
    /**
     * Returns a {@code StringMap} object initialized with the specified properties and the appropriate
     * context data. The returned value may be the specified parameter or a different object.
     * <p>
     * This method will be called for each log event to initialize its context data and implementors should take
     * care to make this method as performant as possible while preserving at least the following thread-safety
     * guarantee.
     * </p><p>
     * Thread-safety note: The returned object can safely be passed off to another thread: future changes in the
     * underlying context data will not be reflected in the returned object.
     * </p><p>
     * Example implementation:
     * </p>
     * <pre>
     * public StringMap injectContextData(List<Property> properties, StringMap reusable) {
     *     if (properties == null || properties.isEmpty()) {
     *         // assume context data is stored in a copy-on-write data structure that is safe to pass to another thread
     *         return (StringMap) rawContextData();
     *     }
     *     // first copy configuration properties into the result
     *     ThreadContextDataInjector.copyProperties(properties, reusable);
     *
     *     // then copy context data key-value pairs (may overwrite configuration properties)
     *     reusable.putAll(rawContextData());
     *     return reusable;
     * }
     * </pre>
     *
     * @param properties Properties from the log4j configuration to be added to the resulting ReadOnlyStringMap. May be
     *          {@code null} or empty
     * @param reusable a {@code StringMap} instance that may be reused to avoid creating temporary objects
     * @return a {@code StringMap} instance initialized with the specified properties and the appropriate
     *          context data. The returned value may be the specified parameter or a different object.
     * @see ThreadContextDataInjector#copyProperties(List, StringMap)
     */
    StringMap injectContextData(final List<Property> properties, final StringMap reusable);

    /**
     * Returns a {@code ReadOnlyStringMap} object reflecting the current state of the context. Configuration properties
     * are not included in the result.
     * <p>
     * This method may be called multiple times for each log event by Filters and Lookups and implementors should take
     * care to make this method as performant as possible while preserving at least the following thread-safety
     * guarantee.
     * </p><p>
     * Thread-safety note: The returned object can only be safely used <em>in the current thread</em>. Changes in the
     * underlying context may or may not be reflected in the returned object, depending on the context data source and
     * the implementation of this method. It is not safe to pass the returned object to another thread.
     * </p>
     * @return a {@code ReadOnlyStringMap} object reflecting the current state of the context, may not return {@code null}
     */
    ReadOnlyStringMap rawContextData();
}
