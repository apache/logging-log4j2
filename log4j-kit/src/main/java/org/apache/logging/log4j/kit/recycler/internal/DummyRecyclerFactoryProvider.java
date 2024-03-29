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
package org.apache.logging.log4j.kit.recycler.internal;

import static java.util.Objects.requireNonNull;

import aQute.bnd.annotation.spi.ServiceProvider;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.recycler.Recycler;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.kit.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.kit.recycler.support.AbstractRecycler;

/**
 * A {@link Recycler} factory provider such that the recycler does not recycle anything; all instances are freshly created.
 *
 * @since 3.0.0
 */
@ServiceProvider(RecyclerFactoryProvider.class)
public final class DummyRecyclerFactoryProvider implements RecyclerFactoryProvider {

    public static final RecyclerFactoryProvider INSTANCE = new DummyRecyclerFactoryProvider();

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public RecyclerFactory createForEnvironment(final PropertyEnvironment environment) {
        return DummyRecyclerFactory.INSTANCE;
    }

    // Visible for testing
    static final class DummyRecyclerFactory implements RecyclerFactory {

        private static final DummyRecyclerFactory INSTANCE = new DummyRecyclerFactory();

        private DummyRecyclerFactory() {}

        @Override
        public <V> Recycler<V> create(final Supplier<V> supplier, final Consumer<V> cleaner) {
            requireNonNull(supplier, "supplier");
            return new DummyRecycler<>(supplier);
        }

        private static final class DummyRecycler<V> extends AbstractRecycler<V> {

            private DummyRecycler(final Supplier<V> supplier) {
                super(supplier);
            }

            @Override
            public V acquire() {
                return createInstance();
            }

            @Override
            public void release(final V value) {}
        }
    }
}
