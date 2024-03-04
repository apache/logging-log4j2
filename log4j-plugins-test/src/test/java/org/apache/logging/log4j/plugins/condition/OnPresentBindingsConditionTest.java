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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Key;
import org.junit.jupiter.api.Test;

class OnPresentBindingsConditionTest {

    static class Dependency {

        Dependency() {}
    }

    static class Dependent {
        final Dependency dependency;

        Dependent(final Dependency dependency) {
            this.dependency = dependency;
        }
    }

    static class DependencyFactory {
        @Factory
        Dependency defaultDependency() {
            return new Dependency();
        }
    }

    static class DependentFactory {
        @Factory
        @ConditionalOnPresentBindings(bindings = Dependency.class)
        Dependent defaultDependent(final Dependency dependency) {
            return new Dependent(dependency);
        }
    }

    final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();

    @Test
    void when_dependency_present_dependent_present() {
        instanceFactory.registerBundle(DependencyFactory.class);
        instanceFactory.registerBundle(DependentFactory.class);
        assertThat(instanceFactory.hasBinding(Key.forClass(Dependent.class))).isTrue();
    }

    @Test
    void when_dependency_absent_dependent_absent() {
        instanceFactory.registerBundle(DependentFactory.class);
        assertThat(instanceFactory.hasBinding(Key.forClass(Dependent.class))).isFalse();
    }
}
