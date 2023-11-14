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
package org.apache.logging.log4j.plugins.condition;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnMissingBindingConditionTest {

    static class Bean {
        final String name;

        Bean(final String name) {
            this.name = name;
        }
    }

    static class DefaultFactory {
        @Factory
        Bean defaultBean() {
            return new Bean("default");
        }
    }

    static class ConditionalFactory {
        @ConditionalOnMissingBinding
        @Namespace("foo")
        @Named("bar")
        @Factory
        Bean bean() {
            return new Bean("backup");
        }
    }

    static class UnconditionalFactory {
        @Namespace("foo")
        @Named("bar")
        @Factory
        Bean unconditionalBean() {
            return new Bean("foobar");
        }
    }

    static class Fixture {
        @Inject Bean defaultValue;
        @Namespace("foo") @Named("bar") Bean value;
    }

    final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();

    @BeforeEach
    void setUp() {
        instanceFactory.registerBundle(DefaultFactory.class);
    }

    @Test
    void whenMissingShouldUseConditionalFactory() {
        instanceFactory.registerBundle(ConditionalFactory.class);
        final Fixture fixture = instanceFactory.getInstance(Fixture.class);
        assertThat(fixture.value).hasFieldOrPropertyWithValue("name", "backup");
        assertThat(fixture.defaultValue).hasFieldOrPropertyWithValue("name", "default");
    }

    @Test
    void whenPresentShouldNotUseConditionalFactory() {
        instanceFactory.registerBundle(UnconditionalFactory.class);
        instanceFactory.registerBundle(ConditionalFactory.class);
        final Fixture fixture = instanceFactory.getInstance(Fixture.class);
        assertThat(fixture.value).hasFieldOrPropertyWithValue("name", "foobar");
        assertThat(fixture.defaultValue).hasFieldOrPropertyWithValue("name", "default");
    }
}
