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
package org.apache.logging.log4j.layout.template.json.util;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverter;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverterRegistry;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.assertj.core.api.Assertions;
import org.jctools.queues.MpmcArrayQueue;
import org.junit.jupiter.api.Test;

class RecyclerFactoriesTest {

    @Test
    void test_RecyclerFactoryConverter() throws Exception {

        // Check if the type converter is registered.
        final TypeConverter<?> converter =
                TypeConverterRegistry.getInstance().findCompatibleConverter(RecyclerFactory.class);
        Assertions.assertThat(converter).isNotNull();

        // Check dummy recycler factory.
        {
            final Object actualDummyRecyclerFactory = converter.convert("dummy");
            Assertions.assertThat(actualDummyRecyclerFactory).isSameAs(DummyRecyclerFactory.getInstance());
        }

        // Check thread-local recycler factory.
        {
            final Object actualThreadLocalRecyclerFactory = converter.convert("threadLocal");
            Assertions.assertThat(actualThreadLocalRecyclerFactory).isSameAs(ThreadLocalRecyclerFactory.getInstance());
        }

        // Check queueing recycler factory.
        {
            final Object actualQueueingRecyclerFactory = converter.convert("queue");
            Assertions.assertThat(actualQueueingRecyclerFactory).isInstanceOf(QueueingRecyclerFactory.class);
        }

        // Check queueing recycler factory with supplier.
        {
            final Object recyclerFactory = converter.convert("queue:supplier=java.util.ArrayDeque.new");
            Assertions.assertThat(recyclerFactory).isInstanceOf(QueueingRecyclerFactory.class);
            final QueueingRecyclerFactory queueingRecyclerFactory = (QueueingRecyclerFactory) recyclerFactory;
            final Recycler<Object> recycler = queueingRecyclerFactory.create(Object::new);
            Assertions.assertThat(recycler).isInstanceOf(QueueingRecycler.class);
            final QueueingRecycler<Object> queueingRecycler = (QueueingRecycler<Object>) recycler;
            Assertions.assertThat(queueingRecycler.getQueue()).isInstanceOf(ArrayDeque.class);
        }

        // Check queueing recycler factory with capacity.
        {
            final Object actualQueueingRecyclerFactory = converter.convert("queue:capacity=100");
            Assertions.assertThat(actualQueueingRecyclerFactory).isInstanceOf(QueueingRecyclerFactory.class);
        }

        // Check queueing recycler factory with supplier and capacity.
        {
            final Object recyclerFactory = converter.convert(
                    "queue:" + "supplier=java.util.concurrent.ArrayBlockingQueue.new," + "capacity=100");
            Assertions.assertThat(recyclerFactory).isInstanceOf(QueueingRecyclerFactory.class);
            final QueueingRecyclerFactory queueingRecyclerFactory = (QueueingRecyclerFactory) recyclerFactory;
            final Recycler<Object> recycler = queueingRecyclerFactory.create(Object::new);
            Assertions.assertThat(recycler).isInstanceOf(QueueingRecycler.class);
            final QueueingRecycler<Object> queueingRecycler = (QueueingRecycler<Object>) recycler;
            Assertions.assertThat(queueingRecycler.getQueue()).isInstanceOf(ArrayBlockingQueue.class);
            final ArrayBlockingQueue<Object> queue = (ArrayBlockingQueue<Object>) queueingRecycler.getQueue();
            Assertions.assertThat(queue.remainingCapacity()).isEqualTo(100);
        }
    }

    @Test
    @LoggerContextSource("recyclerFactoryCustomizedJsonTemplateLayoutLogging.xml")
    void test_RecyclerFactoryConverter_using_XML_config(final @Named(value = "List") ListAppender appender)
            throws Exception {
        final JsonTemplateLayout layout = (JsonTemplateLayout) appender.getLayout();
        final Field field = JsonTemplateLayout.class.getDeclaredField("contextRecycler");
        field.setAccessible(true);
        final QueueingRecycler<?> contextRecycler = (QueueingRecycler<?>) field.get(layout);
        final MpmcArrayQueue<?> queue = (MpmcArrayQueue<?>) contextRecycler.getQueue();
        Assertions.assertThat(queue.capacity()).isEqualTo(512);
    }
}
