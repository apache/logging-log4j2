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

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.ProviderUtil;

/**
 * Creates the ThreadContextMap instance used by the ThreadContext.
 * <p>
 * If {@link Constants#ENABLE_THREADLOCALS Log4j can use ThreadLocals}, a garbage-free StringMap-based context map can
 * be installed by setting system property {@code log4j2.garbagefree.threadContextMap} to {@code true}.
 * </p><p>
 * Furthermore, any custom {@code ThreadContextMap} can be installed by setting system property
 * {@code log4j2.threadContextMap} to the fully qualified class name of the class implementing the
 * {@code ThreadContextMap} interface. (Also implement the {@code ReadOnlyThreadContextMap} interface if your custom
 * {@code ThreadContextMap} implementation should be accessible to applications via the
 * {@link ThreadContext#getThreadContextMap()} method.)
 * </p><p>
 * Instead of system properties, the above can also be specified in a properties file named
 * {@code log4j2.component.properties} in the classpath.
 * </p>
 *
 * @see ThreadContextMap
 * @see ReadOnlyThreadContextMap
 * @see org.apache.logging.log4j.ThreadContext
 * @since 2.7
 */
public final class ThreadContextMapFactory {

    /**
     * Initializes static variables based on system properties. Normally called when this class is initialized by the VM
     * and when Log4j is reconfigured.
     */
    public static void init() {
        ProviderUtil.getProvider().getThreadContextMapInstance();
    }

    private ThreadContextMapFactory() {}

    public static ThreadContextMap createThreadContextMap() {
        return ProviderUtil.getProvider().getThreadContextMapInstance();
    }
}
