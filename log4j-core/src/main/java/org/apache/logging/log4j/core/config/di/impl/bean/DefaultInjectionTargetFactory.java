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

package org.apache.logging.log4j.core.config.di.impl.bean;

import org.apache.logging.log4j.plugins.api.Inject;
import org.apache.logging.log4j.plugins.api.PostConstruct;
import org.apache.logging.log4j.plugins.api.PreDestroy;
import org.apache.logging.log4j.core.config.di.DefinitionException;
import org.apache.logging.log4j.core.config.di.api.bean.Bean;
import org.apache.logging.log4j.core.config.di.api.bean.InjectionTarget;
import org.apache.logging.log4j.core.config.di.api.bean.InjectionTargetFactory;
import org.apache.logging.log4j.core.config.di.api.bean.Injector;
import org.apache.logging.log4j.core.config.di.api.model.ElementManager;
import org.apache.logging.log4j.core.config.di.api.model.InjectionPoint;
import org.apache.logging.log4j.core.config.di.api.model.MetaClass;
import org.apache.logging.log4j.core.config.di.api.model.MetaConstructor;
import org.apache.logging.log4j.core.config.di.api.model.MetaField;
import org.apache.logging.log4j.core.config.di.api.model.MetaMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

class DefaultInjectionTargetFactory<T> implements InjectionTargetFactory<T> {
    private final ElementManager elementManager;
    private final Injector injector;
    private final MetaClass<T> type;

    DefaultInjectionTargetFactory(final ElementManager elementManager, final Injector injector,
                                  final MetaClass<T> type) {
        this.elementManager = elementManager;
        this.injector = injector;
        this.type = type;
    }

    @Override
    public InjectionTarget<T> createInjectionTarget(final Bean<T> bean) {
        final MetaConstructor<T> constructor = getInjectableConstructor();
        final Collection<InjectionPoint> injectionPoints =
                new HashSet<>(elementManager.createExecutableInjectionPoints(constructor, bean));
        for (final MetaField<T, ?> field : type.getFields()) {
            if (elementManager.isInjectable(field)) {
                // TODO: if field is static, validate it's using an appropriate scope (singleton?)
                injectionPoints.add(elementManager.createFieldInjectionPoint(field, bean));
            }
        }
        final List<MetaMethod<T, ?>> methods = new ArrayList<>();
        for (final MetaMethod<T, ?> method : type.getMethods()) {
            methods.add(0, method);
            if (!method.isStatic() && elementManager.isInjectable(method)) {
                injectionPoints.addAll(elementManager.createExecutableInjectionPoints(method, bean));
            }
        }
        // FIXME: verify these methods are ordered properly
        final List<MetaMethod<T, ?>> postConstructMethods = methods.stream()
                .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                .collect(Collectors.toList());
        final List<MetaMethod<T, ?>> preDestroyMethods = methods.stream()
                .filter(method -> method.isAnnotationPresent(PreDestroy.class))
                .collect(Collectors.toList());
        return new DefaultInjectionTarget<>(injector, type, injectionPoints, constructor,
                postConstructMethods, preDestroyMethods);
    }

    private MetaConstructor<T> getInjectableConstructor() {
        final Collection<MetaConstructor<T>> allConstructors = type.getConstructors();
        final List<MetaConstructor<T>> injectConstructors = allConstructors.stream()
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (injectConstructors.size() > 1) {
            throw new DefinitionException("Found more than one constructor with @Inject for " + type);
        }
        if (injectConstructors.size() == 1) {
            return injectConstructors.get(0);
        }
        final List<MetaConstructor<T>> injectParameterConstructors = allConstructors.stream()
                .filter(constructor -> constructor.getParameters().stream().anyMatch(elementManager::isInjectable))
                .collect(Collectors.toList());
        if (injectParameterConstructors.size() > 1) {
            throw new DefinitionException("No @Inject constructors found and remaining constructors ambiguous for " + type);
        }
        if (injectParameterConstructors.size() == 1) {
            return injectParameterConstructors.get(0);
        }
        if (allConstructors.size() == 1) {
            final MetaConstructor<T> constructor = allConstructors.iterator().next();
            if (constructor.getParameters().size() == 0) {
                return constructor;
            }
        }
        return type.getDefaultConstructor()
                .orElseThrow(() -> new DefinitionException("No candidate constructors found for " + type));
    }
}
