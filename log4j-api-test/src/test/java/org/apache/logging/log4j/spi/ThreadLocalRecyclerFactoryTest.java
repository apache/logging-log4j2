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

import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.params.IntRangeSource;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadLocalRecyclerFactoryTest {

    private static class RecyclableObject {}

    private Recycler<RecyclableObject> recycler;

    private Queue<RecyclableObject> recyclerQueue;

    @BeforeEach
    void setUp() {
        recycler = ThreadLocalRecyclerFactory.getInstance().create(RecyclableObject::new);
        recyclerQueue = ((ThreadLocalRecyclerFactory.ThreadLocalRecycler<RecyclableObject>) recycler).getQueue();
    }

    @ParameterizedTest
    @IntRangeSource(from = 1, to = ThreadLocalRecyclerFactory.MAX_QUEUE_SIZE, closed = true)
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

        // simulate a massively callstack with tons of logging
        final List<RecyclableObject> acquiredObjects = IntStream.range(0, 1024)
                .mapToObj(i -> recycler.acquire())
                .collect(Collectors.toList());

        // still nothing returned to pool
        assertThat(recyclerQueue).isEmpty();

        // don't want any duplicate instances
        assertThat(acquiredObjects).containsOnlyOnceElementsOf(acquiredObjects);
        acquiredObjects.forEach(recycler::release);

        // upon return, we should only have ThreadLocalRecyclerFactory.MAX_QUEUE_SIZE retained for future use
        assertThat(recyclerQueue).hasSize(ThreadLocalRecyclerFactory.MAX_QUEUE_SIZE);

    }

}
