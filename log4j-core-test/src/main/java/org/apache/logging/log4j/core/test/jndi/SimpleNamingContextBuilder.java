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
package org.apache.logging.log4j.core.test.jndi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.naming.Context;

/**
 * Builds an in-memory JNDI context for unit tests. Replaces the removed
 * {@code org.springframework.mock.jndi.SimpleNamingContextBuilder} from Spring Framework 6.
 *
 * @since 2.27.0
 */
@SuppressWarnings("BanJNDI")
public final class SimpleNamingContextBuilder {

    private static final ThreadLocal<Map<String, Object>> CURRENT_BINDINGS = new ThreadLocal<>();

    private final Map<String, Object> bindings;

    private SimpleNamingContextBuilder(final Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    /**
     * Creates and activates an empty in-memory JNDI context for the current thread.
     */
    public static SimpleNamingContextBuilder emptyActivatedContextBuilder() {
        final Map<String, Object> bindings = new HashMap<>();
        CURRENT_BINDINGS.set(bindings);
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, SimpleNamingContextFactory.class.getName());
        return new SimpleNamingContextBuilder(bindings);
    }

    /**
     * Binds an object to a JNDI name in the active context.
     */
    public void bind(final String name, final Object value) {
        bindings.put(name, value);
    }

    /**
     * Deactivates the current thread's in-memory JNDI context.
     */
    public void deactivate() {
        CURRENT_BINDINGS.remove();
        System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
    }

    static Map<String, Object> getCurrentBindings() {
        final Map<String, Object> bindings = CURRENT_BINDINGS.get();
        return bindings != null ? bindings : Collections.emptyMap();
    }
}
