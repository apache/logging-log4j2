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
package org.apache.log4j;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;

/**
 * This class behaves just like Log4j's MDC would - and so can cause issues with the redeployment of web
 * applications if the Objects stored in the threads Map cannot be garbage collected.
 */
public final class MDC {


    private static ThreadLocal<Map<String, Object>> localMap =
        new InheritableThreadLocal<Map<String, Object>>() {
            @Override
            protected Map<String, Object> initialValue() {
                return new HashMap<>();
            }

            @Override
            protected Map<String, Object> childValue(final Map<String, Object> parentValue) {
                return parentValue == null ? new HashMap<String, Object>() : new HashMap<>(parentValue);
            }
        };

    private MDC() {
    }


    public static void put(final String key, final String value) {
        localMap.get().put(key, value);
        ThreadContext.put(key, value);
    }


    public static void put(final String key, final Object value) {
        localMap.get().put(key, value);
        ThreadContext.put(key, value.toString());
    }

    public static Object get(final String key) {
        return localMap.get().get(key);
    }

    public static void remove(final String key) {
        localMap.get().remove(key);
        ThreadContext.remove(key);
    }

    public static void clear() {
        localMap.get().clear();
        ThreadContext.clearMap();
    }

    public static Hashtable<String, Object> getContext() {
        return new Hashtable<>(localMap.get());
    }
}
