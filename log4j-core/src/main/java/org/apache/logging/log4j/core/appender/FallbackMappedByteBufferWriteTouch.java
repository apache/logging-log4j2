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

class FallbackMappedByteBufferWriteTouch implements MappedByteBufferWriteTouch {

    private int dummy;

    @Override
    public void writeTouch(final MemoryMappedFileManager.Region region, final int bufferOffset, final int len) {
        int offsetLimit = Math.min(bufferOffset + len, region.mappedBuffer.capacity());
        for (int touchOffset = bufferOffset; touchOffset < offsetLimit; touchOffset += PAGE_SIZE) {
            if (Thread.interrupted()) {
                return;
            }
            if (!region.mappedBufferSystemOperationsLock.readLock().tryLock()) {
                return;
            }
            try {
                if (region.mappedBufferUnmapped) {
                    return;
                }
                dummy ^= region.mappedBuffer.get(touchOffset);
            } finally {
                region.mappedBufferSystemOperationsLock.readLock().unlock();
            }
        }
    }
}
