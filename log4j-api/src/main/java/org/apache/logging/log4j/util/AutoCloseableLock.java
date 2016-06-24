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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps a Lock and makes this wrapper useable in a try-with-resources statement.
 * 
 * @since 2.6.2
 */
public final class AutoCloseableLock implements AutoCloseable, Serializable {
    
    private static final long serialVersionUID = 1L;

    public static AutoCloseableLock wrap(final Lock lock) {
        return new AutoCloseableLock(lock);
    }
    
    public static AutoCloseableLock forReentrantLock() {
        return wrap(new ReentrantLock());
    }
    
    private final Lock lock;

    public AutoCloseableLock(final Lock lock) {
        Objects.requireNonNull(lock, "lock");
        this.lock = lock;
    }

    @Override
    public void close() {
        this.lock.unlock();
    }

    public AutoCloseableLock lock() {
        this.lock.lock();
        return this;
    }

    public AutoCloseableLock lockInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
        return this;
    }

    public Condition newCondition() {
        return this.lock.newCondition();
    }

    public void unlock() {
        this.lock.unlock();
    }
}