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

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps a Lock and makes this wrapper useable in a try-with-resources statement.
 * 
 * Alternative class names: AutoLock, LockResource, LockWrapper.
 * 
 * @since 2.7
 */
public final class AutoCloseableLock implements AutoCloseable, Lock, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new wrapper for a new {@link ReentrantLock}.
     * 
     * @return a new wrapper for a new {@link ReentrantLock}.
     */
    public static AutoCloseableLock forReentrantLock() {
        return wrap(new ReentrantLock());
    }

    /**
     * Wraps the given Lock.
     * 
     * @param lock
     * @return a new wrapper
     */
    public static AutoCloseableLock wrap(final Lock lock) {
        return new AutoCloseableLock(lock);
    }

    private final Lock lock;

    private AutoCloseableLock(final Lock lock) {
        Objects.requireNonNull(lock, "lock");
        this.lock = lock;
    }

    /**
     * Delegates to {@link Lock#lock()}.
     * 
     * @return this
     */
    public AutoCloseableLock autoLock() {
        this.lock.lock();
        return this;
    }

    /**
     * Delegates to {@link Lock#lockInterruptibly()}, use in a try block.
     * 
     * @return this
     * @throws InterruptedException
     */
    public AutoCloseableLock autoLockInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
        return this;
    }

    /**
     * Delegates to {@link Lock#unlock()}.
     */
    @Override
    public void close() {
        this.lock.unlock();
    }

    /**
     * Delegates to {@link Lock#lock()}.
     */
    @Override
    public void lock() {
        this.lock.lock();
    }

    /**
     * Delegates to {@link Lock#lockInterruptibly()}, use in a try block.
     * 
     * @throws InterruptedException
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
    }

    /**
     * Delegates to {@link Lock#newCondition()}.
     * 
     * @return See {@link Lock#newCondition()}.
     */
    @Override
    public Condition newCondition() {
        return this.lock.newCondition();
    }

    /**
     * Delegates to {@link Lock#tryLock()}, do NOT use in a try block.
     * 
     * @return See {@link Lock#tryLock()}.
     */
    @Override
    public boolean tryLock() {
        return this.lock.tryLock();
    }

    /**
     * Delegates to {@link Lock#tryLock()}, do NOT use in a try block.
     * 
     * @param time
     *            See {@link Lock#tryLock()}.
     * @param unit
     *            See {@link Lock#tryLock()}.
     * @return See {@link Lock#tryLock()}.
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return this.lock.tryLock(time, unit);
    }

    /**
     * Delegates to {@link Lock#unlock()}.
     */
    @Override
    public void unlock() {
        this.lock.unlock();
    }
}