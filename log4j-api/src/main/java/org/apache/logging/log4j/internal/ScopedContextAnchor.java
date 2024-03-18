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
package org.apache.logging.log4j.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import org.apache.logging.log4j.ScopedContext;

/**
 * Anchor for the ScopedContext. This class is private and not for public consumption.
 */
public class ScopedContextAnchor {
    private static final ThreadLocal<Deque<ScopedContext>> scopedContext = new ThreadLocal<>();

    /**
     * Returns an immutable Map containing all the key/value pairs as Renderable objects.
     * @return An immutable copy of the Map at the current scope.
     */
    public static Optional<ScopedContext> getContext() {
        Deque<ScopedContext> stack = scopedContext.get();
        if (stack != null) {
            return Optional.of(stack.getFirst());
        }
        return Optional.empty();
    }

    /**
     * Add the ScopeContext.
     * @param context The ScopeContext.
     */
    public static void addScopedContext(ScopedContext context) {
        Deque<ScopedContext> stack = scopedContext.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            scopedContext.set(stack);
        }
        stack.addFirst(context);
    }

    /**
     * Remove the top ScopeContext.
     */
    public static void removeScopedContext() {
        Deque<ScopedContext> stack = scopedContext.get();
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
