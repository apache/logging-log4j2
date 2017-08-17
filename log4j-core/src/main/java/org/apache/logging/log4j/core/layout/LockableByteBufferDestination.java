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
package org.apache.logging.log4j.core.layout;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;

/**
 * The difference between this interface and it's parent {@link ByteBufferDestination} is that for calling {@link
 * #getByteBuffer()} and {@link #drain(ByteBuffer)} the {@link #getDestinationLock()} must be obtained, instead of
 * the intrinsic lock of the destination object, as for generic {@link ByteBufferDestination}. Taking the intrinsic
 * lock on the LockableByteBufferDestination makes no effect and the results of {@link #getByteBuffer()} and {@link
 * #drain(ByteBuffer)} calls are still undefined. The safe usage pattern is:
 * <pre>{@code
 * Lock destinationLock = destination.getDestinationLock();
 * destinationLock.lock();
 * try {
 *     ByteBuffer buffer = destination.getByteBuffer();
 *     ...
 *     buffer = destination.drain(buffer);
 *     ...
 * } finally {
 *     destinationLock.unlock();
 * }
 * }</pre>
 */
public interface LockableByteBufferDestination extends ByteBufferDestination
{
  /**
   * Returns a *reentrant* lock to be used to protect {@link #getByteBuffer()} and {@link #drain(ByteBuffer)} calls on
   * this LockableByteBufferDestination object.
   */
  Lock getDestinationLock();
}
