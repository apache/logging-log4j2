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
package org.apache.logging.log4j.spi;

import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

public class RecyclerFactoriesTest {

    @Test
    void DummyRecyclerFactory_should_work() {
        final RecyclerFactory actualDummyRecyclerFactory = RecyclerFactories.ofSpec("dummy");
        Assertions
                .assertThat(actualDummyRecyclerFactory)
                .isSameAs(DummyRecyclerFactory.getInstance());
    }

    @Test
    void ThreadLocalRecyclerFactory_should_work() {
        final RecyclerFactory actualThreadLocalRecyclerFactory = RecyclerFactories.ofSpec("threadLocal");
        Assertions
                .assertThat(actualThreadLocalRecyclerFactory)
                .asInstanceOf(InstanceOfAssertFactories.type(ThreadLocalRecyclerFactory.class))
                .extracting(ThreadLocalRecyclerFactory::getCapacity)
                .isEqualTo(RecyclerFactories.DEFAULT_QUEUE_CAPACITY);
    }

    @Test
    void ThreadLocalRecyclerFactory_should_work_with_capacity() {
        final RecyclerFactory actualThreadLocalRecyclerFactory = RecyclerFactories.ofSpec("threadLocal:capacity=13");
        Assertions
                .assertThat(actualThreadLocalRecyclerFactory)
                .asInstanceOf(InstanceOfAssertFactories.type(ThreadLocalRecyclerFactory.class))
                .extracting(ThreadLocalRecyclerFactory::getCapacity)
                .isEqualTo(13);
    }

    @Test
    void QueueingRecyclerFactory_should_work() {
        final RecyclerFactory actualQueueingRecyclerFactory = RecyclerFactories.ofSpec("queue");
        Assertions
                .assertThat(actualQueueingRecyclerFactory)
                .isInstanceOf(QueueingRecyclerFactory.class);
    }

    @Test
    void QueueingRecyclerFactory_should_work_with_supplier() {
        final RecyclerFactory recyclerFactory = RecyclerFactories.ofSpec("queue:supplier=java.util.ArrayDeque.new");
        Assertions
                .assertThat(recyclerFactory)
                .isInstanceOf(QueueingRecyclerFactory.class);
        final QueueingRecyclerFactory queueingRecyclerFactory = (QueueingRecyclerFactory) recyclerFactory;
        final Recycler<Object> recycler = queueingRecyclerFactory.create(Object::new);
        Assertions
                .assertThat(recycler)
                .isInstanceOf(QueueingRecyclerFactory.QueueingRecycler.class);
        final QueueingRecyclerFactory.QueueingRecycler<Object> queueingRecycler =
                (QueueingRecyclerFactory.QueueingRecycler<Object>) recycler;
        Assertions
                .assertThat(queueingRecycler.getQueue())
                .isInstanceOf(ArrayDeque.class);
    }

    @Test
    void QueueingRecyclerFactory_should_work_with_capacity() {
        final RecyclerFactory actualQueueingRecyclerFactory = RecyclerFactories.ofSpec("queue:capacity=100");
        Assertions
                .assertThat(actualQueueingRecyclerFactory)
                .isInstanceOf(QueueingRecyclerFactory.class);
    }

    @Test
    void QueueingRecyclerFactory_should_work_with_supplier_and_capacity() {
        final RecyclerFactory recyclerFactory = RecyclerFactories.ofSpec(
                "queue:" +
                        "supplier=java.util.concurrent.ArrayBlockingQueue.new," +
                        "capacity=100");
        Assertions
                .assertThat(recyclerFactory)
                .isInstanceOf(QueueingRecyclerFactory.class);
        final QueueingRecyclerFactory queueingRecyclerFactory = (QueueingRecyclerFactory) recyclerFactory;
        final Recycler<Object> recycler = queueingRecyclerFactory.create(Object::new);
        Assertions
                .assertThat(recycler)
                .isInstanceOf(QueueingRecyclerFactory.QueueingRecycler.class);
        final QueueingRecyclerFactory.QueueingRecycler<Object> queueingRecycler =
                (QueueingRecyclerFactory.QueueingRecycler<Object>) recycler;
        Assertions
                .assertThat(queueingRecycler.getQueue())
                .isInstanceOf(ArrayBlockingQueue.class);
        final ArrayBlockingQueue<Object> queue = (ArrayBlockingQueue<Object>) queueingRecycler.getQueue();
        Assertions.assertThat(queue.remainingCapacity()).isEqualTo(100);

    }

}
