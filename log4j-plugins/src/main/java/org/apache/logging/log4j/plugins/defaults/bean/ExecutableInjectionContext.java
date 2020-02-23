/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.plugins.defaults.bean;

import org.apache.logging.log4j.plugins.spi.bean.BeanManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaParameter;
import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

abstract class ExecutableInjectionContext<T> implements InjectionContext<T> {
    private final BeanManager beanManager;

    private final List<MetaParameter<?>> parameters;
    private final Collection<InjectionPoint<?>> injectionPoints;

    ExecutableInjectionContext(final BeanManager beanManager, final Collection<InjectionPoint<?>> injectionPoints,
                               final List<MetaParameter<?>> parameters) {
        this.beanManager = beanManager;
        this.parameters = parameters;
        this.injectionPoints = injectionPoints;
    }

    private <P> Optional<InjectionPoint<P>> getInjectionPoint(final MetaParameter<P> parameter) {
        return injectionPoints.stream()
                .filter(point -> parameter.equals(point.getElement()))
                .findAny()
                .map(TypeUtil::cast);
    }

    <P> P getInjectableReference(final MetaParameter<P> parameter, final InitializationContext<?> context) {
        return getInjectionPoint(parameter)
                .flatMap(point -> beanManager.getInjectableValue(point, context))
                .orElseThrow(() -> new UnsupportedOperationException("TODO: primitives and defaults"));
    }

    Object[] createArguments(final InitializationContext<?> context) {
        return parameters.stream().map(parameter -> getInjectableReference(parameter, context)).toArray();
    }
}
