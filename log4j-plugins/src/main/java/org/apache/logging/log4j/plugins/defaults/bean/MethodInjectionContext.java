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

import org.apache.logging.log4j.plugins.api.Disposes;
import org.apache.logging.log4j.plugins.spi.bean.BeanManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.model.MetaParameter;
import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.util.Collection;

class MethodInjectionContext<D, T> extends ExecutableInjectionContext<T> {
    private final MetaMethod<D, T> method;
    private final D declaringInstance;
    private final Object producerInstance;

    MethodInjectionContext(final BeanManager beanManager, final Collection<InjectionPoint<?>> injectionPoints,
                           final MetaMethod<D, T> method, final D declaringInstance, final Object producerInstance) {
        super(beanManager, injectionPoints, method.getParameters());
        this.method = method;
        this.declaringInstance = declaringInstance;
        this.producerInstance = producerInstance;
    }

    @Override
    <P> P getInjectableReference(final MetaParameter<P> parameter, final InitializationContext<?> context) {
        return parameter.isAnnotationPresent(Disposes.class) ? TypeUtil.cast(producerInstance) : super.getInjectableReference(parameter, context);
    }

    @Override
    public T invoke(final InitializationContext<?> context) {
        return method.invoke(declaringInstance, createArguments(context));
    }
}
