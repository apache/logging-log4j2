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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.kit.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.kit.recycler.internal.DummyRecyclerFactoryProvider.DummyRecyclerFactory;
import org.apache.logging.log4j.kit.recycler.internal.QueueingRecyclerFactoryProvider.QueueingRecyclerFactory;
import org.apache.logging.log4j.kit.recycler.internal.ThreadLocalRecyclerFactoryProvider.ThreadLocalRecyclerFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

public class RecyclerFactoryRegistryTest {

    private static final int DEFAULT_CAPACITY =
            Math.max(2 * Runtime.getRuntime().availableProcessors() + 1, 8);

    @Test
    void DummyRecyclerFactory_should_work() {
        final RecyclerFactory factory = RecyclerFactoryTestUtil.createForEnvironment("dummy", null);
        assertThat(factory).isInstanceOf(DummyRecyclerFactory.class);
    }

    @Test
    void ThreadLocalRecyclerFactory_should_work() {
        final RecyclerFactory factory = RecyclerFactoryTestUtil.createForEnvironment("threadLocal", null);
        assertThat(factory)
                .asInstanceOf(InstanceOfAssertFactories.type(ThreadLocalRecyclerFactory.class))
                .extracting(factory_ -> factory_.capacity)
                .isEqualTo(DEFAULT_CAPACITY);
    }

    @Test
    void ThreadLocalRecyclerFactory_should_work_with_capacity() {
        final int capacity = 13;
        final RecyclerFactory factory = RecyclerFactoryTestUtil.createForEnvironment("threadLocal", capacity);
        assertThat(factory)
                .asInstanceOf(InstanceOfAssertFactories.type(ThreadLocalRecyclerFactory.class))
                .extracting(factory_ -> factory_.capacity)
                .isEqualTo(capacity);
    }

    @Test
    void QueueingRecyclerFactory_should_work() {
        final RecyclerFactory factory = RecyclerFactoryTestUtil.createForEnvironment("queue", null);
        assertThat(factory)
                .asInstanceOf(InstanceOfAssertFactories.type(QueueingRecyclerFactory.class))
                .extracting(factory_ -> factory_.capacity)
                .isEqualTo(DEFAULT_CAPACITY);
    }

    @Test
    void QueueingRecyclerFactory_should_work_with_capacity() {
        final int capacity = 100;
        final RecyclerFactory factory = RecyclerFactoryTestUtil.createForEnvironment("queue", capacity);
        assertThat(factory)
                .asInstanceOf(InstanceOfAssertFactories.type(QueueingRecyclerFactory.class))
                .extracting(factory_ -> factory_.capacity)
                .isEqualTo(capacity);
    }

    @Test
    void verify_order() {
        final RecyclerFactoryProvider dummyProvider = new DummyRecyclerFactoryProvider();
        final RecyclerFactoryProvider threadLocalProvider = new ThreadLocalRecyclerFactoryProvider();
        final RecyclerFactoryProvider queueProvider = new QueueingRecyclerFactoryProvider();
        assertThat(dummyProvider.getOrder()).isGreaterThan(queueProvider.getOrder());
        assertThat(queueProvider.getOrder()).isGreaterThan(threadLocalProvider.getOrder());
    }
}
