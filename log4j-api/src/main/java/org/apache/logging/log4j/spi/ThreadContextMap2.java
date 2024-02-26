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

import java.util.Map;
import org.apache.logging.log4j.util.StringMap;

/**
 * Extension service provider interface to implement additional custom MDC behavior for
 * {@link org.apache.logging.log4j.ThreadContext}.
 *
 * Consider implementing {@link CleanableThreadContextMap} instead.
 *
 * @see ThreadContextMap
 * @since 2.7
 */
public interface ThreadContextMap2 extends ThreadContextMap {

    /**
     * Puts all given context map entries into the current thread's
     * context map.
     *
     * <p>If the current thread does not have a context map it is
     * created as a side effect.</p>
     * @param map The map.
     * @since 2.7
     */
    void putAll(final Map<String, String> map);

    /**
     * Returns the context data for reading. Note that regardless of whether the returned context data has been
     * {@linkplain StringMap#freeze() frozen} (made read-only) or not, callers should not attempt to modify
     * the returned data structure.
     *
     * @return the {@code StringMap}
     */
    StringMap getReadOnlyContextData();
}
