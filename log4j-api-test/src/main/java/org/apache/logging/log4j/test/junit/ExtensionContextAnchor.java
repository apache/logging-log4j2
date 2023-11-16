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
package org.apache.logging.log4j.test.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public class ExtensionContextAnchor
        implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    public static Namespace LOG4J2_NAMESPACE = Namespace.create("org.apache.logging.log4j.junit");
    private static final ThreadLocal<ExtensionContext> EXTENSION_CONTEXT = new InheritableThreadLocal<>();

    private static void bind(final ExtensionContext context) {
        EXTENSION_CONTEXT.set(context);
    }

    private static void unbind(final ExtensionContext context) {
        EXTENSION_CONTEXT.set(context.getParent().orElse(null));
    }

    public static ExtensionContext getContext() {
        return EXTENSION_CONTEXT.get();
    }

    public static ExtensionContext getContext(final ExtensionContext context) {
        return context != null ? context : EXTENSION_CONTEXT.get();
    }

    public static <T> T getAttribute(final Object key, final Class<T> clazz, final ExtensionContext context) {
        final ExtensionContext actualContext = getContext(context);
        assertNotNull(actualContext, "missing ExtensionContext");
        return actualContext.getStore(LOG4J2_NAMESPACE).get(key, clazz);
    }

    public static void setAttribute(final Object key, final Object value, final ExtensionContext context) {
        final ExtensionContext actualContext = getContext(context);
        assertNotNull(actualContext, "missing ExtensionContext");
        actualContext.getStore(LOG4J2_NAMESPACE).put(key, value);
    }

    public static void removeAttribute(final Object key, final ExtensionContext context) {
        final ExtensionContext actualContext = getContext(context);
        if (actualContext != null) {
            actualContext.getStore(LOG4J2_NAMESPACE).remove(key);
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        unbind(context);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        unbind(context);
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        bind(context);
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        bind(context);
    }
}
