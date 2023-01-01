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
package org.apache.logging.log4j.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ThreadLocalRecyclerTest {

    @Test
    void referenceCountingEnabledTracking() {
        final ThreadLocalRecycler<AtomicInteger> recycler =
                new ThreadLocalRecycler<>(AtomicInteger::new, i -> i.set(0), true);

        assertThat(recycler.getActiveReferenceCount()).isEqualTo(0);
        final AtomicInteger first = recycler.acquire();
        assertThat(recycler.getActiveReferenceCount()).isEqualTo(1);
        final AtomicInteger second = recycler.acquire();
        assertThat(recycler.getActiveReferenceCount()).isEqualTo(2);
        first.set(1);
        second.set(2);
        final AtomicInteger third = recycler.acquire();
        assertThat(recycler.getActiveReferenceCount()).isEqualTo(3);
        assertThat(third.get()).isEqualTo(0);
        assertThat(first.get()).isEqualTo(1);
        assertThat(second.get()).isEqualTo(2);
        recycler.release(first);
        assertThat(recycler.getActiveReferenceCount()).isEqualTo(2);
        recycler.release(second);
        assertThat(recycler.getActiveReferenceCount()).isEqualTo(1);
        recycler.release(third);
        assertThat(recycler.getActiveReferenceCount()).isEqualTo(0);
    }
}
