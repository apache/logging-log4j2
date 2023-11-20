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
import static org.assertj.core.api.Assertions.from;

import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.test.validation.di.CustomQualifier;
import org.apache.logging.log4j.plugins.test.validation.di.SingletonBean;
import org.junit.jupiter.api.Test;

class QualifierInjectionTest {
    static class TestBundle {
        @Factory
        @Named("alpha")
        SingletonBean alphaSingletonBean() {
            return new SingletonBean();
        }

        @Factory
        @CustomQualifier
        SingletonBean qualifiedSingletonBean() {
            return new SingletonBean();
        }
    }

    static class TestBean {
        final Supplier<SingletonBean> defaultBeanFactory;
        final SingletonBean defaultBean;
        final Supplier<SingletonBean> alphaBeanFactory;
        final SingletonBean alphaBean;
        final Supplier<SingletonBean> qualifiedBeanFactory;
        final SingletonBean qualifiedBean;

        @Inject
        TestBean(
                final Supplier<SingletonBean> defaultBeanFactory,
                final SingletonBean defaultBean,
                @Named("alpha") final Supplier<SingletonBean> alphaBeanFactory,
                @Named("alpha") final SingletonBean alphaBean,
                @CustomQualifier final Supplier<SingletonBean> qualifiedBeanFactory,
                @CustomQualifier final SingletonBean qualifiedBean) {
            this.defaultBeanFactory = defaultBeanFactory;
            this.defaultBean = defaultBean;
            this.alphaBeanFactory = alphaBeanFactory;
            this.alphaBean = alphaBean;
            this.qualifiedBeanFactory = qualifiedBeanFactory;
            this.qualifiedBean = qualifiedBean;
        }
    }

    final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();

    @Test
    void qualifiedInjection() {
        instanceFactory.registerBundle(TestBundle.class);
        final TestBean bean = instanceFactory.getInstance(TestBean.class);
        assertThat(bean.defaultBean).isNotNull().isNotSameAs(bean.alphaBean).isNotSameAs(bean.qualifiedBean);
        assertThat(bean.defaultBeanFactory).isNotNull().returns(bean.defaultBean, from(Supplier::get));
        assertThat(bean.alphaBean).isNotNull().isNotSameAs(bean.defaultBean).isNotSameAs(bean.qualifiedBean);
        assertThat(bean.alphaBeanFactory).isNotNull().returns(bean.alphaBean, from(Supplier::get));
        assertThat(bean.qualifiedBean).isNotNull().isNotSameAs(bean.defaultBean).isNotSameAs(bean.alphaBean);
        assertThat(bean.qualifiedBeanFactory).isNotNull().returns(bean.qualifiedBean, from(Supplier::get));
    }
}
