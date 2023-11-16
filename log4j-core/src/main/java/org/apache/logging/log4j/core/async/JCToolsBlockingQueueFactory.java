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
package org.apache.logging.log4j.core.async;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.jctools.queues.MpscArrayQueue;

/**
 * Factory for creating instances of BlockingQueues backed by JCTools {@link MpscArrayQueue}.
 *
 * @since 2.7
 */
@Plugin(name = "JCToolsBlockingQueue", category = Node.CATEGORY, elementType = BlockingQueueFactory.ELEMENT_TYPE)
public class JCToolsBlockingQueueFactory<E> implements BlockingQueueFactory<E> {

    private final WaitStrategy waitStrategy;

    private JCToolsBlockingQueueFactory(final WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    @Override
    public BlockingQueue<E> create(final int capacity) {
        return new MpscBlockingQueue<>(capacity, waitStrategy);
    }

    @PluginFactory
    public static <E> JCToolsBlockingQueueFactory<E> createFactory(
            @PluginAttribute(value = "WaitStrategy", defaultString = "PARK") final WaitStrategy waitStrategy) {
        return new JCToolsBlockingQueueFactory<>(waitStrategy);
    }

    /**
     * BlockingQueue wrapper for JCTools multiple producer single consumer array queue.
     */
    private static final class MpscBlockingQueue<E> extends MpscArrayQueue<E> implements BlockingQueue<E> {

        private final JCToolsBlockingQueueFactory.WaitStrategy waitStrategy;

        MpscBlockingQueue(final int capacity, final JCToolsBlockingQueueFactory.WaitStrategy waitStrategy) {
            super(capacity);
            this.waitStrategy = waitStrategy;
        }

        @Override
        public int drainTo(final Collection<? super E> c) {
            return drainTo(c, capacity());
        }

        @Override
        public int drainTo(final Collection<? super E> c, final int maxElements) {
            return drain(e -> c.add(e), maxElements);
        }

        @Override
        public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
            int idleCounter = 0;
            final long timeoutNanos = System.nanoTime() + unit.toNanos(timeout);
            do {
                if (offer(e)) {
                    return true;
                } else if (System.nanoTime() - timeoutNanos > 0) {
                    return false;
                }
                idleCounter = waitStrategy.idle(idleCounter);
            } while (!Thread.interrupted()); // clear interrupted flag
            throw new InterruptedException();
        }

        @Override
        public E poll(final long timeout, final TimeUnit unit) throws InterruptedException {
            int idleCounter = 0;
            final long timeoutNanos = System.nanoTime() + unit.toNanos(timeout);
            do {
                final E result = poll();
                if (result != null) {
                    return result;
                } else if (System.nanoTime() - timeoutNanos > 0) {
                    return null;
                }
                idleCounter = waitStrategy.idle(idleCounter);
            } while (!Thread.interrupted()); // clear interrupted flag
            throw new InterruptedException();
        }

        @Override
        public void put(final E e) throws InterruptedException {
            int idleCounter = 0;
            do {
                if (offer(e)) {
                    return;
                }
                idleCounter = waitStrategy.idle(idleCounter);
            } while (!Thread.interrupted()); // clear interrupted flag
            throw new InterruptedException();
        }

        @Override
        public boolean offer(final E e) {
            // keep 2 cache lines empty to avoid false sharing that will slow the consumer thread when queue is full.
            return offerIfBelowThreshold(e, capacity() - 32);
        }

        @Override
        public int remainingCapacity() {
            return capacity() - size();
        }

        @Override
        public E take() throws InterruptedException {
            int idleCounter = 100;
            do {
                final E result = relaxedPoll();
                if (result != null) {
                    return result;
                }
                idleCounter = waitStrategy.idle(idleCounter);
            } while (!Thread.interrupted()); // clear interrupted flag
            throw new InterruptedException();
        }
    }

    public enum WaitStrategy {
        SPIN(idleCounter -> idleCounter + 1),
        YIELD(idleCounter -> {
            Thread.yield();
            return idleCounter + 1;
        }),
        PARK(idleCounter -> {
            LockSupport.parkNanos(1L);
            return idleCounter + 1;
        }),
        PROGRESSIVE(idleCounter -> {
            if (idleCounter > 200) {
                LockSupport.parkNanos(1L);
            } else if (idleCounter > 100) {
                Thread.yield();
            }
            return idleCounter + 1;
        });

        private final Idle idle;

        private int idle(final int idleCounter) {
            return idle.idle(idleCounter);
        }

        WaitStrategy(final Idle idle) {
            this.idle = idle;
        }
    }

    private interface Idle {
        int idle(int idleCounter);
    }
}
