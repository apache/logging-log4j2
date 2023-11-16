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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * Factory for creating instances of {@link ArrayBlockingQueue}.
 *
 * @since 2.7
 */
@Configurable(elementType = BlockingQueueFactory.ELEMENT_TYPE)
@Plugin("ArrayBlockingQueue")
public class ArrayBlockingQueueFactory implements BlockingQueueFactory {
    @Override
    public <E> BlockingQueue<E> create(final int capacity) {
        return new ArrayBlockingQueue<>(capacity);
    }

    @PluginFactory
    public static ArrayBlockingQueueFactory createFactory() {
        return new ArrayBlockingQueueFactory();
    }
}
