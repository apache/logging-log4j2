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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

/**
 * Factory for ContextDataInjectors. Returns a new {@code ContextDataInjector} instance based on the value of system
 * property {@code log4j2.ContextDataInjector}. Users may use this system property to specify the fully qualified class
 * name of a class that implements the {@code ContextDataInjector} interface.
 * If no value was specified this factory method returns one of the injectors defined in
 * {@code ThreadContextDataInjector}.
 *
 * @see ContextDataInjector
 * @see ReadOnlyStringMap
 * @see ThreadContextDataInjector
 * @see LogEvent#getContextData()
 * @since 2.7
 */
public class ContextDataInjectorFactory {

    private static final String CONTEXT_DATA_INJECTOR_PROPERTY = "log4j2.ContextDataInjector";

    /**
     * Returns a new {@code ContextDataInjector} instance based on the value of system property
     * {@code log4j2.ContextDataInjector}. If no value was specified this factory method returns one of the
     * {@code ContextDataInjector} classes defined in {@link ThreadContextDataInjector} which is most appropriate for
     * the ThreadContext implementation.
     * <p>
     * <b>Note:</b> It is no longer recommended that users provide a custom implementation of the ContextDataInjector.
     * Instead, provide a {@code ContextDataProvider}.
     * </p>
     * <p>
     * Users may use this system property to specify the fully qualified class name of a class that implements the
     * {@code ContextDataInjector} interface.
     * </p><p>
     * When providing a custom {@code ContextDataInjector}, be aware that this method may be invoked multiple times by
     * the various components in Log4j that need access to context data.
     * This includes the object(s) that populate log events, but also various lookups and filters that look at
     * context data to determine whether an event should be logged.
     * </p>
     *
     * @return a ContextDataInjector that populates the {@code ReadOnlyStringMap} of all {@code LogEvent} objects
     * @see LogEvent#getContextData()
     * @see ContextDataInjector
     */
    public static ContextDataInjector createInjector() {
        try {
            return LoaderUtil.newCheckedInstanceOfProperty(
                    CONTEXT_DATA_INJECTOR_PROPERTY,
                    ContextDataInjector.class,
                    ContextDataInjectorFactory::createDefaultInjector);
        } catch (final ReflectiveOperationException e) {
            StatusLogger.getLogger().warn("Could not create ContextDataInjector: {}", e.getMessage(), e);
            return createDefaultInjector();
        }
    }

    private static ContextDataInjector createDefaultInjector() {
        final ReadOnlyThreadContextMap threadContextMap = ThreadContext.getThreadContextMap();

        // note: map may be null (if legacy custom ThreadContextMap was installed by user)
        if (threadContextMap instanceof DefaultThreadContextMap || threadContextMap == null) {
            return new ThreadContextDataInjector.ForDefaultThreadContextMap(); // for non StringMap-based context maps
        }
        if (threadContextMap instanceof CopyOnWrite) {
            return new ThreadContextDataInjector.ForCopyOnWriteThreadContextMap();
        }
        return new ThreadContextDataInjector.ForGarbageFreeThreadContextMap();
    }
}
