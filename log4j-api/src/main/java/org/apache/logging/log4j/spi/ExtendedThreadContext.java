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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.util.ProviderUtil;

/**
 * Provides advanced access method to the {@link ThreadContextMap}.
 * @since 2.24.0
 */
public final class ExtendedThreadContext {

    private ExtendedThreadContext() {}

    /**
     * Saves the context data of the current thread
     * <p>
     *     Thread-safety note: The returned object can safely be passed off to another thread: future changes in the
     *     underlying context data will not be reflected in the returned object.
     * </p>
     * @return An opaque representation of the context data.
     * @see #restoreMap
     */
    public static Object saveMap() {
        return getThreadContextMap().save();
    }

    /**
     * Restores the context data of the current thread from a saved version
     * @param contextMap An opaque representation of the context data obtained through a previous call to
     *                   {@link #saveMap} or {@code restoreMap}.
     * @return An opaque representation of the current thread's context data.
     * @see #saveMap
     */
    public static Object restoreMap(final Object contextMap) {
        return getThreadContextMap().restore(contextMap);
    }

    /**
     * Clears the context data of the current thread
     */
    public static void clearMap() {
        getThreadContextMap().clear();
    }

    private static ThreadContextMap getThreadContextMap() {
        return ProviderUtil.getProvider().getThreadContextMapInstance();
    }
}
