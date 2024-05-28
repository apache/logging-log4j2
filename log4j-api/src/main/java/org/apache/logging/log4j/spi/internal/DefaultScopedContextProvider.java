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
package org.apache.logging.log4j.spi.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import org.apache.logging.log4j.spi.AbstractScopedContextProvider;
import org.apache.logging.log4j.spi.ScopedContextProvider;

/**
 * An implementation of {@link ScopedContextProvider}.
 * @since 2.24.0
 */
public class DefaultScopedContextProvider extends AbstractScopedContextProvider {

    public static final ScopedContextProvider INSTANCE = new DefaultScopedContextProvider();

    private final ThreadLocal<Deque<Instance>> scopedContext = new ThreadLocal<>();

    /**
     * Returns an immutable Map containing all the key/value pairs as Object objects.
     * @return An immutable copy of the Map at the current scope.
     */
    @Override
    protected Optional<Instance> getContext() {
        final Deque<Instance> stack = scopedContext.get();
        return stack != null ? Optional.of(stack.getFirst()) : Optional.empty();
    }

    /**
     * Add the ScopeContext.
     * @param context The ScopeContext.
     */
    @Override
    protected void addScopedContext(final Instance context) {
        Deque<Instance> stack = scopedContext.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            scopedContext.set(stack);
        }
        stack.addFirst(context);
    }

    /**
     * Remove the top ScopeContext.
     */
    @Override
    protected void removeScopedContext() {
        final Deque<Instance> stack = scopedContext.get();
        if (stack != null) {
            if (!stack.isEmpty()) {
                stack.removeFirst();
            }
            if (stack.isEmpty()) {
                scopedContext.remove();
            }
        }
    }
}
