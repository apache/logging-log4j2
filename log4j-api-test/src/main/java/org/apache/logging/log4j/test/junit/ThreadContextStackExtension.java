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
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import java.util.Collection;

class ThreadContextStackExtension implements BeforeEachCallback {
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
    public void beforeEach(ExtensionContext context) throws Exception {
        context.getStore(Namespace.create(ThreadContext.class, context.getRequiredTestClass(), context.getRequiredTestInstance()))
                .getOrComputeIfAbsent(ThreadContextStackStore.class);
    }
}
