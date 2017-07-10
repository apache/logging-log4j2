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

import java.lang.reflect.Field;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

class UnsafeMappedByteBufferWriteTouch implements MappedByteBufferWriteTouch {
    private static final Unsafe UNSAFE;
    static {
        try {
            UNSAFE = AccessController.doPrivileged(new PrivilegedExceptionAction<Unsafe>() {
                @Override
                public Unsafe run() throws Exception {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                }
            });

        }
        catch (Exception e) {
            throw new RuntimeException("Cannot access Unsafe methods", e);
        }
    }

    @Override
    public void writeTouch(final MemoryMappedFileManager.Region region, final int bufferOffset, final int len) {
        MappedByteBuffer buffer = region.mappedBuffer;
        long bufferAddress = ((DirectBuffer) buffer).address();
        long addressLimit = bufferAddress + Math.min(bufferOffset + len, buffer.capacity());
        for (long touchAddress = roundTo4ByteBoundary(bufferAddress + bufferOffset);
             touchAddress < addressLimit - 3;
             touchAddress += PAGE_SIZE) {
            if (Thread.interrupted()) {
                return;
            }
            if (touchAddress >= bufferAddress) {
                if (!region.mappedBufferSystemOperationsLock.readLock().tryLock()) {
                    return;
                }
                try {
                    if (region.mappedBufferUnmapped) {
                        return;
                    }
                    doTouch(touchAddress);
                } finally {
                    region.mappedBufferSystemOperationsLock.readLock().unlock();
                }
            }
        }
    }

    private void doTouch(final long touchAddress) {
        while (true) {
            int x = UNSAFE.getInt(null, touchAddress);
            // Java 7 compatible, cannot use addAndGet
            if (UNSAFE.compareAndSwapInt(null, touchAddress, x, x)) {
                return;
            }
        }
    }

    private static long roundTo4ByteBoundary(long address) {
        return address & ~3;
    }
}
