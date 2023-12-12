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
package org.apache.logging.log4j.api.queue;

import aQute.bnd.annotation.spi.ServiceProvider;
import java.util.Queue;
import org.apache.logging.log4j.spi.QueueFactory;
import org.jctools.queues.MpmcArrayQueue;

/**
 * A multi-producer-multi-consumer, thread-safe {@link QueueFactory} implementation based on <a href="https://jctools.github.io/JCTools/">JCTools</a>.
 */
@ServiceProvider(QueueFactory.class)
public final class JCToolsQueueFactory implements QueueFactory {

    @Override
    public <E> Queue<E> create(final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("invalid capacity: " + capacity);
        }
        return new MpmcArrayQueue<>(capacity);
    }
}
