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
import org.apache.logging.log4j.core.config.di.api.model.MetaConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class DefaultMetaConstructor<T> extends AbstractMetaExecutable<T, T> implements MetaConstructor<T> {
    private final Constructor<T> constructor;

    DefaultMetaConstructor(final MetaClass<T> metaClass, final Constructor<T> constructor) {
        super(metaClass, constructor, metaClass);
        this.constructor = constructor;
    }

    @Override
    public T construct(final Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new InitializationException("Error invoking constructor " + constructor, e);
        } catch (final InvocationTargetException e) {
            throw new InitializationException("Error invoking constructor " + constructor, e.getCause());
        }
    }

    @Override
    public String toString() {
        return constructor.toGenericString();
    }
}
