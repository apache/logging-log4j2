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
package org.apache.logging.log4j.core.appender;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A copy of {@link CountDownLatch} with uninterruptible {@link #await()}. Also this class directly extends {@link
 * AbstractQueuedSynchronizer} (instead of defining an inner class) for less indirection.
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
final class UninterruptibleCountDownLatch extends AbstractQueuedSynchronizer {

    UninterruptibleCountDownLatch(int count) {
        setState(count);
    }

    @Override
    protected int tryAcquireShared(int acquires) {
        return (getState() == 0) ? 1 : -1;
    }

    @Override
    protected boolean tryReleaseShared(int releases) {
        // Decrement count; signal when transition to zero
        for (;;) {
            int c = getState();
            if (c == 0) {
                return false;
            }
            int nextc = c - 1;
            if (compareAndSetState(c, nextc)) {
                return nextc == 0;
            }
        }
    }

    void await() {
        acquireShared(1);
    }

    void countDown() {
        releaseShared(1);
    }
}
