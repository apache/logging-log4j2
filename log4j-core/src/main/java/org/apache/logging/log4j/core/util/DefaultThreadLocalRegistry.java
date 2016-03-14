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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.util.ThreadLocalRegistry;

/**
 * ThreadLocalRegistry.
 */
public class DefaultThreadLocalRegistry implements ThreadLocalRegistry {

    private ConcurrentMap<String, Log4jThreadLocal<?>> threadLocalMap = new ConcurrentHashMap<>();

    @Override
    public ThreadLocal<?> get(String name) {
        Log4jThreadLocal<?> threadLocal = threadLocalMap.get(name);
        if (threadLocal != null) {
            return threadLocal;
        }
        return threadLocalMap.putIfAbsent(name, new Log4jThreadLocal());
    }

    @Override
    public void remove(String name) {
        threadLocalMap.remove(name);
    }

    @Override
    public void clearAndRemoveAll() {
        for (Log4jThreadLocal<?> threadLocal : threadLocalMap.values()) {
            threadLocal.clear();
        }
        threadLocalMap.clear();
    }
}
