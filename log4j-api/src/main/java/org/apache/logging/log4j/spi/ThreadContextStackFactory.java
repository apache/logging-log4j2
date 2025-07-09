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
 * Creates the ThreadContextStack instance used by the ThreadContext.
 * <p>
 * Any custom {@code ThreadContextStack} can be installed by setting system property
 * {@code log4j2.threadContextStack} to the fully qualified class name of the class implementing the
 * {@code ThreadContextStack} interface.
 * </p><p>
 * Instead of system properties, the above can also be specified in a properties file named
 * {@code log4j2.component.properties} in the classpath.
 * </p>
 *
 * @see ThreadContextStack
 * @see org.apache.logging.log4j.ThreadContext
 */
public final class ThreadContextStackFactory {

    /**
     * Initializes static variables based on system properties. Normally called when this class is initialized by the VM
     * and when Log4j is reconfigured.
     */
    public static void init() {
        ProviderUtil.getProvider().getThreadContextStackInstance();
    }

    private ThreadContextStackFactory() {}

    public static ThreadContextStack createThreadContextStack() {
        return ProviderUtil.getProvider().getThreadContextStackInstance();
    }
}
