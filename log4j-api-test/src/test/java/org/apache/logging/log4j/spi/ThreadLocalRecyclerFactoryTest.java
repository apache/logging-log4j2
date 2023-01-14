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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadLocalRecyclerFactoryTest {
    @Test
    void nestedAcquiresDoNotInterfere() {
        final Recycler<AtomicInteger> r = ThreadLocalRecyclerFactory.getInstance()
                .create(AtomicInteger::new, i -> i.set(0));
        final var recycler = (ThreadLocalRecyclerFactory.ThreadLocalRecycler<AtomicInteger>) r;

        assertThat(recycler.getQueue()).isEmpty();
        final AtomicInteger first = recycler.acquire();
        assertThat(recycler.getQueue()).isEmpty();
        final AtomicInteger second = recycler.acquire();
        assertThat(recycler.getQueue()).isEmpty();
        first.set(1);
        second.set(2);
        final AtomicInteger third = recycler.acquire();
        assertThat(recycler.getQueue()).isEmpty();
        assertThat(third.get()).isEqualTo(0);
        assertThat(first.get()).isEqualTo(1);
        assertThat(second.get()).isEqualTo(2);
        recycler.release(first);
        assertThat(recycler.getQueue()).hasSize(1);
        recycler.release(second);
        assertThat(recycler.getQueue()).hasSize(2);
        recycler.release(third);
        assertThat(recycler.getQueue()).hasSize(3);
    }
}
