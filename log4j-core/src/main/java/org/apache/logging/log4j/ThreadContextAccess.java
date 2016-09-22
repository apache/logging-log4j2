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

package org.apache.logging.log4j;

import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextMap2;

/**
 * <em>This class is intended for internal log4j2 usage and should not be used directly by applications.</em>
 * <p>
 * Utility class to access package protected methods in {@code ThreadContext}.
 * </p>
 *
 * @see ThreadContext
 * @since 2.7
 */
public final class ThreadContextAccess {
    private ThreadContextAccess() { // this class should not be instantiated
    }

    /**
     * Returns the internal data structure used to store thread context key-value pairs.
     * <p><em>
     * This method is intended for internal log4j2 usage.
     * The returned data structure is not intended to be used directly by applications.
     * </em></p>
     * @return the internal data structure used to store thread context key-value pairs
     */
    public static ThreadContextMap getThreadContextMap() {
        return ThreadContext.getThreadContextMap();
    }

    /**
     * Returns the internal data structure used to store thread context key-value pairs.
     * <p><em>
     * This method is intended for internal log4j2 usage.
     * The returned data structure is not intended to be used directly by applications.
     * </em></p>
     * @return the internal data structure used to store thread context key-value pairs
     */
    public static ThreadContextMap2 getThreadContextMap2() {
        return (ThreadContextMap2) ThreadContext.getThreadContextMap();
    }
}
