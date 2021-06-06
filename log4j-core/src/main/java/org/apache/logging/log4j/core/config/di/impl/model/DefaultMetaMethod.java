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

package org.apache.logging.log4j.core.config.di.impl.model;

import org.apache.logging.log4j.core.config.di.InitializationException;
import org.apache.logging.log4j.core.config.di.api.model.MetaClass;
import org.apache.logging.log4j.core.config.di.api.model.MetaMethod;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class DefaultMetaMethod<D, T> extends AbstractMetaExecutable<D, T> implements MetaMethod<D, T> {
    private final Method method;

    DefaultMetaMethod(final MetaClass<D> declaringClass, final Method method) {
        super(declaringClass, method, DefaultMetaClass.newMetaClass(method.getGenericReturnType(), TypeUtil.cast(method.getReturnType())));
        this.method = method;
    }

    @Override
    public T invoke(final D target, final Object... args) {
        try {
            return TypeUtil.cast(method.invoke(target, args));
        } catch (final IllegalAccessException e) {
            throw new InitializationException("Error invoking method: " + method + " on target " + target, e);
        } catch (final InvocationTargetException e) {
            throw new InitializationException("Error invoking method: " + method + " on target " + target, e.getCause());
        }
    }

    @Override
    public String toString() {
        return method.toGenericString();
    }
}
