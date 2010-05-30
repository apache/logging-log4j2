/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.logging.log4j;

import java.util.HashMap;
import java.util.Map;


/**
 * The MDC class is similar to the {@link NDC} class except that it is
 * based on a map instead of a stack. It provides <em>mapped
 * diagnostic contexts</em>. A <em>Mapped Diagnostic Context</em>, or
 * MDC in short, is an instrument for distinguishing interleaved log
 * output from different sources. Log output is typically interleaved
 * when a server handles multiple clients near-simultaneously.
 * <p/>
 * <p><b><em>The MDC is managed on a per thread basis</em></b>. A
 * child thread automatically inherits a <em>copy</em> of the mapped
 * diagnostic context of its parent.
 *
 * @doubt I'd throw the concept into a ThreadContext object. (RG) I agree - will revise this.
 */
public final class MDC {

    private static ThreadLocal<Map<String, Object>> LOCAL =
        new InheritableThreadLocal<Map<String, Object>>() {
            protected Map<String, Object> initialValue() {
                return new HashMap<String, Object>();
            }

            protected Map<String, Object> childValue(Map<String, Object> parentValue) {
                return parentValue == null ? null : new HashMap<String, Object>(parentValue);
            }
        };


    private MDC() {

    }

    /**
     * Put a context value (the <code>o</code> parameter) as identified
     * with the <code>key</code> parameter into the current thread's
     * context map.
     * <p/>
     * <p>If the current thread does not have a context map it is
     * created as a side effect.
     * @param key The key name.
     * @param value The key value.
     */
    public static void put(String key, Object value) {
        LOCAL.get().put(key, value);
    }

    /**
     * Get the context identified by the <code>key</code> parameter.
     * <p/>
     * <p>This method has no side effects.
     * @param key The key to locate.
     * @return The value of the object or null.
     */
    public static Object get(String key) {
        return LOCAL.get().get(key);
    }

    /**
     * Remove the the context identified by the <code>key</code>
     * parameter.
     * @param key The key to remove.
     */
    public static void remove(String key) {
        LOCAL.get().remove(key);
    }

    /**
     * Clear the context.
     */
    public static void clear() {
        LOCAL.get().clear();
    }

    /**
     * Determine if the key is in the context.
     * @param key The key to locate.
     * @return True if the key is in the context, false otherwise.
     */
    public static boolean containsKey(String key) {
        return LOCAL.get().containsKey(key);
    }

    /**
     * Get the current thread's MDC as a hashtable. This method is
     * intended to be used internally.
     * @return a copy of the context.
     */
    public static Map<String, Object> getContext() {
        return new HashMap<String, Object>(LOCAL.get());
    }

}
