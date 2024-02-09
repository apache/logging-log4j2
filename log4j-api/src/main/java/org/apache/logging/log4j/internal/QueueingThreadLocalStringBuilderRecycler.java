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
package org.apache.logging.log4j.internal;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Queue;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * A {@link StringBuilderRecycler} that keeps a {@link ThreadLocal} queue to recycle instances.
 * <p>
 * When the queue capacity is exceeded, {@link #acquire()} starts allocating new instances.
 * The queue provides convenience when recursive {@link #acquire()} calls are expected.
 * If not, use {@link ThreadLocalStringBuilderRecycler} instead to avoid the cost of maintaining a queue.
 * </p>
 *
 * @since 2.23.0
 */
final class QueueingThreadLocalStringBuilderRecycler implements StringBuilderRecycler {

    private final ThreadLocal<Queue<StringBuilder>> queueHolder;

    QueueingThreadLocalStringBuilderRecycler(final int threadLocalQueueCapacity) {
        this.queueHolder = ThreadLocal.withInitial(() -> new ArrayQueue<>(threadLocalQueueCapacity));
    }

    @Override
    public StringBuilder acquire() {
        Queue<StringBuilder> queue = queueHolder.get();
        @Nullable final StringBuilder stringBuilder = queue.poll();
        return stringBuilder != null ? stringBuilder : new StringBuilder();
    }

    @Override
    public void release(final StringBuilder stringBuilder) {
        StringBuilders.trimToMaxSize(stringBuilder, MAX_STRING_BUILDER_CAPACITY);
        stringBuilder.setLength(0);
        final Queue<StringBuilder> queue = queueHolder.get();
        queue.offer(stringBuilder);
    }
}
