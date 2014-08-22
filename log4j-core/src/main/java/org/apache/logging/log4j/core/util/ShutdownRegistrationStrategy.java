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

package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Strategy used for registering shutdown hook threads. Due to differing requirements of how late in the JVM lifecycle
 * Log4j should be shut down, this interface is provided for customizing how to register shutdown hook threads.
 *
 * @since 2.1
 */
public interface ShutdownRegistrationStrategy {

    /**
     * System property to set to choose the ShutdownRegistryStrategy.
     */
    String SHUTDOWN_REGISTRATION_STRATEGY = "log4j.shutdownRegistrationStrategy";

    /**
     * System property to set to override the global ability to register shutdown hooks.
     */
    String SHUTDOWN_HOOK_ENABLED = "log4j.shutdownHookEnabled";

    /**
     * Shared Marker to indicate log messages corresponding to shutdown hooks.
     */
    Marker SHUTDOWN_HOOK_MARKER = MarkerManager.getMarker("SHUTDOWN HOOK");

    /**
     * Adds a shutdown hook to be executed upon JVM exit.
     *
     * @param hook a Thread in the {@link Thread.State#NEW} state
     * @throws IllegalStateException If the virtual machine is already in the process of shutting down
     */
    void registerShutdownHook(Thread hook);

    /**
     * Removes a shutdown hook.
     *
     * @param hook a previously registered shutdown hook Thread.
     * @throws IllegalStateException If the virtual machine is already in the process of shutting down
     */
    void unregisterShutdownHook(Thread hook);
}
