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
import org.apache.logging.log4j.util.StringBuilders;

/**
 * A {@link StringBuilderRecycler} that stores a single {@link StringBuilder} in a {@link ThreadLocal}.
 * <p>
 * This implementation causes new instance creation when {@link #acquire()} is called recursively.
 * If your use case needs recursion support, prefer {@link QueueingThreadLocalStringBuilderRecycler} instead.
 * </p>
 *
 * @since 2.23.0
 */
final class ThreadLocalStringBuilderRecycler implements StringBuilderRecycler {

    private final int maxLength;

    private final ThreadLocal<StringBuilder> stringBuilderHolder = ThreadLocal.withInitial(StringBuilder::new);

    ThreadLocalStringBuilderRecycler(final int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public StringBuilder acquire() {
        @Nullable StringBuilder stringBuilder = stringBuilderHolder.get();
        if (stringBuilder == null) {
            return new StringBuilder();
        }
        // noinspection ThreadLocalSetWithNull
        stringBuilderHolder.set(null);
        return stringBuilder;
    }

    @Override
    public void release(final StringBuilder stringBuilder) {
        StringBuilders.trimToMaxSize(stringBuilder, maxLength);
        stringBuilder.setLength(0);
        stringBuilderHolder.set(stringBuilder);
    }
}
