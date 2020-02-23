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
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;

class FieldInjectionContext<D, T> implements InjectionContext<T> {
    private final BeanManager beanManager;

    private final InjectionPoint<T> injectionPoint;
    private final MetaField<D, T> field;
    private final D declaringInstance;

    FieldInjectionContext(final BeanManager beanManager, final InjectionPoint<T> injectionPoint,
                          final MetaField<D, T> field, final D declaringInstance) {
        this.beanManager = beanManager;
        this.injectionPoint = injectionPoint;
        this.field = field;
        this.declaringInstance = declaringInstance;
    }

    @Override
    public T invoke(final InitializationContext<?> context) {
        beanManager.getInjectableValue(injectionPoint, context)
                .ifPresent(value -> field.set(declaringInstance, value));
        return field.get(declaringInstance);
    }
}
