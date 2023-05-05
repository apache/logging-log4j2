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

import java.util.concurrent.BlockingQueue;

/**
 * Factory for creating instances of {@link BlockingQueue}.
 *
 * @since 2.7
 */
public interface BlockingQueueFactory<E> {

    /**
     * The {@link org.apache.logging.log4j.core.config.plugins.Plugin#elementType() element type} to use for plugins
     * implementing this interface.
     */
    String ELEMENT_TYPE = "BlockingQueueFactory";

    /**
     * Creates a new BlockingQueue with the specified maximum capacity. Note that not all implementations of
     * BlockingQueue support a bounded capacity in which case the value is ignored.
     *
     * @param capacity maximum size of the queue if supported
     * @return a new BlockingQueue
     */
    BlockingQueue<E> create(int capacity);
}
