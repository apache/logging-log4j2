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

import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.kit.recycler.internal.ThreadLocalRecyclerFactoryProvider.ThreadLocalRecyclerFactory;
import org.apache.logging.log4j.kit.recycler.internal.ThreadLocalRecyclerFactoryProvider.ThreadLocalRecyclerFactory.ThreadLocalRecycler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.params.IntRangeSource;

class ThreadLocalRecyclerFactoryProviderTest {

    private static final int CAPACITY = 13;

    private static class RecyclableObject {}

    private ThreadLocalRecycler<RecyclableObject> recycler;

    private Queue<RecyclableObject> recyclerQueue;

    @BeforeEach
    void setUp() {
        final RecyclerFactory recyclerFactory = RecyclerFactoryTestUtil.createForEnvironment("threadLocal", CAPACITY);
        assertThat(recyclerFactory).isInstanceOf(ThreadLocalRecyclerFactory.class);
        assert recyclerFactory != null;
        recycler = (ThreadLocalRecycler<RecyclableObject>) recyclerFactory.create(RecyclableObject::new);
        recyclerQueue = recycler.queueRef.get();
    }

    @ParameterizedTest
    @IntRangeSource(from = 1, to = CAPACITY, closed = true)
    void nested_acquires_should_not_interfere(final int acquisitionCount) {

        // pool should start empty
        assertThat(recyclerQueue).isEmpty();

        final List<RecyclableObject> acquiredObjects = IntStream.range(0, acquisitionCount)
                .mapToObj(i -> recycler.acquire())
                .collect(Collectors.toList());

        // still nothing returned to pool
        assertThat(recyclerQueue).isEmpty();

        // don't want any duplicate instances
        assertThat(acquiredObjects).containsOnlyOnceElementsOf(acquiredObjects);
        acquiredObjects.forEach(recycler::release);

        // and now they should be back in the pool
        assertThat(recyclerQueue).hasSize(acquisitionCount);

        // then reacquire them to see that they're still the same object as we've filled in
        // the thread-local queue with returned objects
        final List<RecyclableObject> reacquiredObjects = IntStream.range(0, acquisitionCount)
                .mapToObj(i -> recycler.acquire())
                .collect(Collectors.toList());

        assertThat(reacquiredObjects).containsExactlyElementsOf(acquiredObjects);
    }

    @Test
    void nested_acquires_past_max_queue_size_should_discard_extra_releases() {

        assertThat(recyclerQueue).isEmpty();

        // Simulate a callstack with excessive logging
        final int acquisitionCount = Math.addExact(CAPACITY, 1024);
        final List<RecyclableObject> acquiredObjects = IntStream.range(0, acquisitionCount)
                .mapToObj(i -> recycler.acquire())
                .toList();

        // Verify collected instances are all new
        assertThat(acquiredObjects).doesNotHaveDuplicates();

        // Verify the pool is still empty
        assertThat(recyclerQueue).isEmpty();

        // Release all acquired instances
        acquiredObjects.forEach(recycler::release);

        // Verify the queue size is capped
        assertThat(recyclerQueue).hasSize(CAPACITY);
    }
}
