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

import org.apache.logging.log4j.plugins.spi.model.MetaClass;
import org.apache.logging.log4j.plugins.spi.bean.Bean;
import org.apache.logging.log4j.plugins.spi.model.Qualifiers;
import org.apache.logging.log4j.plugins.spi.model.Variable;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

abstract class SystemBean<T> implements Bean<T> {
    private final Variable<T> variable;

    SystemBean(final Variable<T> variable) {
        this.variable = variable;
    }

    @Override
    public Qualifiers getQualifiers() {
        return variable.getQualifiers();
    }

    @Override
    public Collection<Type> getTypes() {
        return variable.getTypes();
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        return variable.getScopeType();
    }

    @Override
    public Collection<InjectionPoint<?>> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public MetaClass<T> getDeclaringClass() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void destroy(final T instance, final InitializationContext<T> context) {
        context.close();
    }
}
