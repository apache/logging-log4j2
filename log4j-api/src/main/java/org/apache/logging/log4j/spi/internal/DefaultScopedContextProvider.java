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

import java.util.Optional;
import org.apache.logging.log4j.spi.AbstractScopedContextProvider;
import org.apache.logging.log4j.spi.ScopedContextProvider;

/**
 * An implementation of {@link ScopedContextProvider} that uses the simplest implementation.
 * @since 2.24.0
 */
public class DefaultScopedContextProvider extends AbstractScopedContextProvider {

    public static final ScopedContextProvider INSTANCE = new DefaultScopedContextProvider();

    private final ThreadLocal<MapInstance> scopedContext = new ThreadLocal<>();

    /**
     * Returns an immutable Map containing all the key/value pairs as Object objects.
     * @return The current context Instance.
     */
    protected Optional<Instance> getContext() {
        return Optional.ofNullable(scopedContext.get());
    }

    /**
     * Add the ScopeContext.
     * @param context The ScopeContext.
     */
    protected void addScopedContext(final MapInstance context) {
        scopedContext.set(context);
    }

    /**
     * Remove the top ScopeContext.
     */
    protected void removeScopedContext() {
        MapInstance current = scopedContext.get();
        if (current == null) {
            return;
        }
        MapInstance previous = current.getPrevious();
        if (previous != null) {
            scopedContext.set(previous);
        } else {
            scopedContext.remove();
        }
    }
}
