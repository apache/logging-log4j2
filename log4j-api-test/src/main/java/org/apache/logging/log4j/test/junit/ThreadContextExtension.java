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

package org.apache.logging.log4j.test.junit;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.util.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

class ThreadContextExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    private static class ThreadContextMapStore implements Store.CloseableResource {
        private final Map<String, String> previousMap = ThreadContext.getImmutableContext();

        private ThreadContextMapStore() {
            ThreadContext.clearMap();
        }

        @Override
        public void close() throws Throwable {
            // TODO LOG4J2-1517 Add ThreadContext.setContext(Map<String, String>)
            ThreadContext.clearMap();
            ThreadContext.putAll(previousMap);
        }
    }

    private static class ThreadContextStackStore implements Store.CloseableResource {
        private final Collection<String> previousStack = ThreadContext.getImmutableStack();

        private ThreadContextStackStore() {
            ThreadContext.clearStack();
        }

        @Override
        public void close() throws Throwable {
            ThreadContext.setStack(previousStack);
        }
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final Store store = getTestStore(context);
        if (AnnotationUtils.isAnnotated(testClass, UsingThreadContextMap.class)) {
            store.put(ThreadContextMapStore.class, new ThreadContextMapStore());
        }
        if (AnnotationUtils.isAnnotated(testClass, UsingThreadContextStack.class)) {
            store.put(ThreadContextStackStore.class, new ThreadContextStackStore());
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final Store store = getTestStore(context);
        final Method testMethod = context.getRequiredTestMethod();
        if (AnnotationUtils.isAnnotated(testMethod, UsingThreadContextMap.class)) {
            store.put(ThreadContextMapStore.class, new ThreadContextMapStore());
        }
        if (AnnotationUtils.isAnnotated(testMethod, UsingThreadContextStack.class)) {
            store.put(ThreadContextStackStore.class, new ThreadContextStackStore());
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationUtils.isAnnotated(testClass, UsingThreadContextMap.class)) {
            ThreadContext.clearMap();
        }
        if (AnnotationUtils.isAnnotated(testClass, UsingThreadContextStack.class)) {
            ThreadContext.clearMap();
        }
    }

    private static Store getTestStore(final ExtensionContext context) {
        final Namespace baseNamespace = Namespace.create(ThreadContext.class, context.getRequiredTestClass());
        final Namespace namespace = context.getTestInstance().map(baseNamespace::append).orElse(baseNamespace);
        return context.getStore(namespace);
    }
}
