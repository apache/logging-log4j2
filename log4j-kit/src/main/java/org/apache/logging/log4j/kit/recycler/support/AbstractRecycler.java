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
package org.apache.logging.log4j.kit.recycler.support;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;
import org.apache.logging.log4j.kit.recycler.Recycler;
import org.apache.logging.log4j.kit.recycler.RecyclerAware;

/**
 * Abstract implementation of {@link Recycler} that properly handles {@link RecyclerAware} objects
 *
 * @param <V> The type of recycled object.
 * @since 3.0.0
 */
public abstract class AbstractRecycler<V> implements Recycler<V> {

    private final Supplier<V> supplier;

    protected AbstractRecycler(final Supplier<V> supplier) {
        this.supplier = requireNonNull(supplier, "supplier");
    }

    protected final V createInstance() {
        final V instance = supplier.get();
        if (instance instanceof RecyclerAware) {
            @SuppressWarnings("unchecked")
            final RecyclerAware<V> recyclerAware = (RecyclerAware<V>) instance;
            recyclerAware.setRecycler(this);
        }
        return instance;
    }
}
