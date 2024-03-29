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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ArrayQueueTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void invalid_capacity_should_not_be_allowed(final int invalidCapacity) {
        assertThatThrownBy(() -> new ArrayQueue<>(invalidCapacity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid capacity: " + invalidCapacity);
    }

    @Test
    void should_work_with_capacity_1() {

        // Verify initials
        final Queue<String> queue = new ArrayQueue<>(1);
        assertThat(queue.size()).isEqualTo(0);
        assertThat(queue.peek()).isNull();
        assertThat(queue.poll()).isNull();
        assertThat(queue).isEmpty();

        // Verify enqueue & deque
        assertThat(queue.offer("foo")).isTrue();
        assertThat(queue.offer("bar")).isFalse();
        assertThat(queue.size()).isEqualTo(1);
        assertThat(queue).containsOnly("foo");
        assertThat(queue.peek()).isEqualTo("foo");
        assertThat(queue.poll()).isEqualTo("foo");

        // Verify final state
        assertThat(queue.size()).isEqualTo(0);
        assertThat(queue.peek()).isNull();
        assertThat(queue.poll()).isNull();
        assertThat(queue).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "1,0.3", "1,0.5", "1,0.8", "2,0.3", "2,0.5", "2,0.8", "3,0.3", "3,0.5", "3,0.8", "4,0.3", "4,0.5", "4,0.8"
    })
    void ops_should_match_with_std_lib(final int capacity, final double pollRatio) {

        // Set the stage
        final Random random = new Random(0);
        final int opCount = random.nextInt(100);
        final Queue<String> queueRef = new ArrayBlockingQueue<>(capacity);
        final Queue<String> queueTarget = new ArrayQueue<>(capacity);

        for (int opIndex = 0; opIndex < opCount; opIndex++) {

            // Verify entry
            assertThat(queueTarget.size()).isEqualTo(queueRef.size());
            assertThat(queueTarget.peek()).isEqualTo(queueRef.peek());
            assertThat(queueTarget).containsExactlyElementsOf(queueRef);

            // Is this a `poll()`?
            if (pollRatio >= random.nextDouble()) {
                assertThat(queueTarget.poll()).isEqualTo(queueRef.poll());
            }

            // Then this is an `offer()`
            else {
                final String item = "op@" + opIndex;
                assertThat(queueTarget.offer(item)).isEqualTo(queueRef.offer(item));
            }

            // Verify exit
            assertThat(queueTarget.size()).isEqualTo(queueRef.size());
            assertThat(queueTarget.peek()).isEqualTo(queueRef.peek());
            assertThat(queueTarget).containsExactlyElementsOf(queueRef);
        }
    }
}
