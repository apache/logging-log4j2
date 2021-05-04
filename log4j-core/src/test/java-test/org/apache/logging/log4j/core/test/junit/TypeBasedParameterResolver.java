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
package org.apache.logging.log4j.core.test.junit;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * An adapter that resolved a parameter based on its type.
 */
public abstract class TypeBasedParameterResolver<T> implements ParameterResolver {

    private final Type parameterType;

    public TypeBasedParameterResolver() {
        this.parameterType = enclosedTypeOfParameterResolver();
    }

    @Override
    public final boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return this.parameterType.equals(getParameterType(parameterContext));
    }

    @Override
    public abstract T resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException;

    private Type getParameterType(ParameterContext parameterContext) {
        return parameterContext.getParameter().getParameterizedType();
    }

    private Type enclosedTypeOfParameterResolver() {
        ParameterizedType typeBasedParameterResolverSuperclass = findTypeBasedParameterResolverSuperclass(getClass());
        if (typeBasedParameterResolverSuperclass == null) {
            String msg = String.format("Failed to discover parameter type supported by %s; "
                            + "potentially caused by lacking parameterized type in class declaration.",
                    getClass().getName());
            fail(msg);
        }
        return typeBasedParameterResolverSuperclass.getActualTypeArguments()[0];
    }

    private ParameterizedType findTypeBasedParameterResolverSuperclass(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return null;
        }

        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) genericSuperclass).getRawType();
            if (rawType == TypeBasedParameterResolver.class) {
                return (ParameterizedType) genericSuperclass;
            }
        }
        return findTypeBasedParameterResolverSuperclass(superclass);
    }

}
