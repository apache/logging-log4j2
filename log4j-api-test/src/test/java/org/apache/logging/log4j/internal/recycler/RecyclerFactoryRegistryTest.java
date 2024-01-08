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
package org.apache.logging.log4j.internal.recycler;

import static org.apache.logging.log4j.spi.recycler.Recycler.DEFAULT_CAPACITY;
import static org.assertj.core.api.Assertions.assertThat;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Properties;
import org.apache.logging.log4j.internal.recycler.DummyRecyclerFactoryProvider.DummyRecyclerFactory;
import org.apache.logging.log4j.internal.recycler.QueueingRecyclerFactoryProvider.QueueingRecyclerFactory;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.spi.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.spi.recycler.RecyclerFactoryRegistry;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

public class RecyclerFactoryRegistryTest {

    @Test
    void DummyRecyclerFactory_should_work() {
        final RecyclerFactory factory = createForEnvironment("dummy", null);
        assertThat(factory).isInstanceOf(DummyRecyclerFactory.class);
    }

    @Test
    void QueueingRecyclerFactory_should_work() {
        final RecyclerFactory factory = createForEnvironment("queue", null);
        assertThat(factory)
                .asInstanceOf(InstanceOfAssertFactories.type(QueueingRecyclerFactory.class))
                .extracting(factory_ -> factory_.capacity)
                .isEqualTo(DEFAULT_CAPACITY);
    }

    @Test
    void QueueingRecyclerFactory_should_work_with_capacity() {
        final int capacity = 100;
        final RecyclerFactory factory = createForEnvironment("queue", capacity);
        assertThat(factory)
                .asInstanceOf(InstanceOfAssertFactories.type(QueueingRecyclerFactory.class))
                .extracting(factory_ -> factory_.capacity)
                .isEqualTo(capacity);
    }

    @Nullable
    private static RecyclerFactory createForEnvironment(
            @Nullable final String factory, @Nullable final Integer capacity) {
        final Properties properties = new Properties();
        if (factory != null) {
            properties.setProperty("log4j2.*.Recycler.factory", factory);
        }
        if (capacity != null) {
            properties.setProperty("log4j2.*.Recycler.capacity", "" + capacity);
        }
        final PropertyEnvironment env = new PropertiesUtil(properties);
        return RecyclerFactoryRegistry.findRecyclerFactory(env);
    }

    @Test
    void verify_order() {
        final RecyclerFactoryProvider dummyProvider = new DummyRecyclerFactoryProvider();
        final RecyclerFactoryProvider queueProvider = new QueueingRecyclerFactoryProvider();
        assertThat(dummyProvider.getOrder()).isGreaterThan(queueProvider.getOrder());
    }
}
