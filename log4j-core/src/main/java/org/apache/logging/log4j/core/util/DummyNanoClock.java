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
package org.apache.logging.log4j.core.util;

/**
 * Implementation of the {@code NanoClock} interface that always returns a fixed value.
 */
public final class DummyNanoClock implements NanoClock {

    private final long fixedNanoTime;

    public DummyNanoClock() {
        this(0L);
    }

    /**
     * Constructs a new DummyNanoClock with the specified value to return.
     * @param fixedNanoTime the value to return from {@link #nanoTime()}.
     */
    public DummyNanoClock(final long fixedNanoTime) {
        this.fixedNanoTime = fixedNanoTime;
    }

    /**
     * Returns the constructor value.
     *
     * @return the constructor value
     */
    @Override
    public long nanoTime() {
        return fixedNanoTime;
    }
}
