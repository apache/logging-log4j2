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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

public class AutoCloseableLockTest {

    @Test(timeout = 100)
    public void sanityCheck() {
        final Lock lock = new ReentrantLock();
        lock.lock();
        lock.unlock();
    }

    @Test(timeout = 100)
    public void testLockInTryWithResources() {
        final AutoCloseableLock lock = AutoCloseableLock.forReentrantLock();
        try (AutoCloseableLock l = lock.autoLock()) {
            // ...
        }
        lock.tryLock();
        lock.unlock();
    }

    @Test(timeout = 100)
    public void testNewLockInTryWithResources() {
        try (AutoCloseableLock l = AutoCloseableLock.forReentrantLock().autoLock()) {
            // ...
        }
    }

    @Test(timeout = 100)
    public void testNewLockWithTryWithResources() {
        final AutoCloseableLock localLock = AutoCloseableLock.forReentrantLock().autoLock();
        try (AutoCloseableLock l = localLock) {
            // ...
        }
    }
}
