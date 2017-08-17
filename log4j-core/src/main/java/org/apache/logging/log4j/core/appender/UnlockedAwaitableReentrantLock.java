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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A copy of non-fair version of {@link ReentrantLock} which allows to await for unlocked state via {@link
 * #awaitUnlocked()}. Also this class directly extends {@link AbstractQueuedSynchronizer} (instead of defining an inner
 * class) for less indirection.
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
final class UnlockedAwaitableReentrantLock extends AbstractQueuedSynchronizer implements Lock {

    /**
     * Prohibits the lock acquisition when the parental {@link MemoryMappedFileManager} is closing or already closed.
     */
    boolean memoryMappedFileManagerIsClosing = false;

    /**
     * Performs lock.  Try immediate barge, backing up to normal
     * acquire on failure.
     */
    @Override
    public void lock() {
        acquire(1);
    }

    @Override
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (Thread.currentThread() != getExclusiveOwnerThread()) {
            throw new IllegalMonitorStateException();
        }
        boolean free = false;
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }

    boolean isHeldByCurrentThread() {
        return isHeldExclusively();
    }

    @Override
    protected final boolean isHeldExclusively() {
        // While we must in general read state before owner,
        // we don't need to do so to check if current thread is owner
        return getExclusiveOwnerThread() == Thread.currentThread();
    }

    /** Non-fair */
    @Override
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                if (memoryMappedFileManagerIsClosing) {
                    setState(0);
                    throw new IllegalStateException("MemoryMappedFileManager is closed");
                }
                setExclusiveOwnerThread(current);
                return true;
            }
        } else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        return tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        release(1);
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("Conditions are not supported by this lock");
    }

    void awaitUnlocked() {
        if (getState() == 0) {
            return;
        }
        acquireShared(1);
    }

    @Override
    protected int tryAcquireShared(final int arg) {
        return getState() == 0 ? 1 : -1;
    }
}
