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
package org.apache.logging.log4j.core;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import org.junit.jupiter.api.function.ThrowingSupplier;

public final class GcHelper {

    private GcHelper() {}

    /**
     * Waits for the value to be garbage collected.
     *
     * @param valueSupplier a value provider
     */
    @SuppressWarnings({"unused", "UnusedAssignment"})
    public static void awaitGarbageCollection(final ThrowingSupplier<?> valueSupplier) throws InterruptedException {

        // Create the reference queue
        final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
        final Reference<?> ref;
        try {
            final Object value = valueSupplier.get();
            ref = new PhantomReference<>(value, refQueue);
        } catch (final Throwable error) {
            throw new RuntimeException("failed obtaining value", error);
        }

        // Wait for the garbage collection
        try (final GcPressureGenerator ignored = GcPressureGenerator.ofStarted()) {
            final Reference<?> removedRef = refQueue.remove(30_000L);
            if (removedRef == null) {
                throw new AssertionError("garbage collector did not reclaim the value");
            }
        }
    }
}
