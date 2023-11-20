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
package org.apache.logging.log4j.plugins.di;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.test.validation.di.PrototypeBean;
import org.apache.logging.log4j.plugins.test.validation.di.SingletonBean;
import org.junit.jupiter.api.Test;

class InstanceFactoryTest {
    final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();

    @Test
    void canLoadDefaultConstructorInstanceOnDemand() {
        final SingletonBean bean = instanceFactory.getInstance(SingletonBean.class);
        assertThat(bean).isNotNull();
    }

    @Test
    void singletonInstancesAreSameInstance() {
        final Supplier<SingletonBean> factory = instanceFactory.getFactory(SingletonBean.class);
        assertThat(factory.get()).isSameAs(factory.get());
    }

    @Test
    void unscopedInstancesFreshlyCreatedEachTime() {
        final Supplier<PrototypeBean> factory = instanceFactory.getFactory(PrototypeBean.class);
        assertThat(factory.get()).isNotNull().isNotSameAs(factory.get());
    }
}
