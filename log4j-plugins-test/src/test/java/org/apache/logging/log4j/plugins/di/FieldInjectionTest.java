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

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.test.validation.di.AnotherSingletonBean;
import org.apache.logging.log4j.plugins.test.validation.di.CustomQualifier;
import org.apache.logging.log4j.plugins.test.validation.di.PrototypeChildBean;
import org.apache.logging.log4j.plugins.test.validation.di.PrototypeGrandchildBean;
import org.apache.logging.log4j.plugins.test.validation.di.SingletonBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldInjectionTest {
    static class TestBundle {
        @Factory
        @CustomQualifier
        PrototypeGrandchildBean prototypeGrandchildBean(PrototypeGrandchildBean unqualified) {
            return unqualified;
        }
    }

    static class TestBean {
        @Inject
        SingletonBean singletonBean;

        @Inject
        AnotherSingletonBean anotherSingletonBean;

        @CustomQualifier
        PrototypeGrandchildBean prototypeGrandchildBean;
    }

    final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();

    @BeforeEach
    void setUp() {
        instanceFactory.registerBundle(TestBundle.class);
    }

    @Test
    void fieldInjection() {
        final TestBean bean = instanceFactory.getInstance(TestBean.class);
        assertThat(bean).isNotNull();
        assertThat(bean.singletonBean).isNotNull();
        assertThat(bean.anotherSingletonBean).isNotNull();
        assertThat(bean.prototypeGrandchildBean).isNotNull()
                .extracting(PrototypeGrandchildBean::getPrototypeChildBean)
                .isNotNull()
                .extracting(PrototypeChildBean::getPrototypeBean)
                .isNotNull();
    }
}
