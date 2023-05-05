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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Registry used for Runnable shutdown callback instances. Due to differing requirements of how late in the JVM
 * lifecycle Log4j should be shut down, this interface is provided for customizing how to register shutdown hook
 * callbacks. Implementations may optionally implement {@link org.apache.logging.log4j.core.LifeCycle}.
 *
 * @since 2.1
 */
public interface ShutdownCallbackRegistry {

    /**
     * System property to set to choose the ShutdownCallbackRegistry.
     */
    String SHUTDOWN_CALLBACK_REGISTRY = "log4j.shutdownCallbackRegistry";

    /**
     * System property to set to override the global ability to register shutdown hooks.
     */
    String SHUTDOWN_HOOK_ENABLED = "log4j.shutdownHookEnabled";

    /**
     * Shared Marker to indicate log messages corresponding to shutdown hooks.
     */
    Marker SHUTDOWN_HOOK_MARKER = MarkerManager.getMarker("SHUTDOWN HOOK");

    /**
     * Adds a Runnable shutdown callback to this class.
     *
     * Note: The returned {@code Cancellable} must be retained on heap by caller
     * to avoid premature garbage-collection of the registered callback (and to ensure
     * the callback runs on shutdown).
     *
     * @param callback the shutdown callback to be executed upon shutdown.
     * @return a Cancellable wrapper of the provided callback or {@code null} if the shutdown hook is disabled and
     * cannot be added.
     * @since 2.1
     */
    Cancellable addShutdownCallback(Runnable callback);
}
