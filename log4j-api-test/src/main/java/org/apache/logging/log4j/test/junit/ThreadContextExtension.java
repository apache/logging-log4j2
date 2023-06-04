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

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.test.ThreadContextHolder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class ThreadContextExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final ThreadContextHolder holder;
        if (testClass.isAnnotationPresent(UsingAnyThreadContext.class)) {
            holder = new ThreadContextHolder(true, true);
            ThreadContext.clearAll();
        } else if (testClass.isAnnotationPresent(UsingThreadContextMap.class)) {
            holder = new ThreadContextHolder(true, false);
            ThreadContext.clearMap();
        } else if (testClass.isAnnotationPresent(UsingThreadContextStack.class)) {
            holder = new ThreadContextHolder(false, true);
            ThreadContext.clearStack();
        } else {
            return;
        }
        getStore(context).put(ThreadContextHolder.class, holder);
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        final ThreadContextHolder holder = getStore(context).get(ThreadContextHolder.class, ThreadContextHolder.class);
        if (holder != null) {
            holder.restore();
        }
    }

    private ExtensionContext.Store getStore(final ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestInstance()));
    }
}
