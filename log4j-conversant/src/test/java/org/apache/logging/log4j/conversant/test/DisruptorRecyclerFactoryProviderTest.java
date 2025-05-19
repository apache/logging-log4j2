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
package org.apache.logging.log4j.conversant.test;

import static org.assertj.core.api.Assertions.assertThat;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import org.apache.logging.log4j.conversant.DisruptorRecyclerFactoryProvider;
import org.apache.logging.log4j.kit.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.junit.jupiter.api.Test;

@ServiceConsumer(value = RecyclerFactoryProvider.class, resolution = Cardinality.MULTIPLE)
class DisruptorRecyclerFactoryProviderTest {

    @Test
    void verify_is_the_first() {
        final List<Class<?>> providerClasses = ServiceLoaderUtil.safeStream(
                        RecyclerFactoryProvider.class,
                        ServiceLoader.load(
                                RecyclerFactoryProvider.class, getClass().getClassLoader()),
                        StatusLogger.getLogger())
                .sorted(Comparator.comparing(RecyclerFactoryProvider::getOrder))
                .<Class<?>>map(RecyclerFactoryProvider::getClass)
                .toList();
        assertThat(providerClasses).startsWith(DisruptorRecyclerFactoryProvider.class);
    }
}
