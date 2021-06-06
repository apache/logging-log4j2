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
import org.apache.logging.log4j.core.config.di.api.model.MetaField;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.reflect.Field;

public class DefaultMetaField<D, T> extends AbstractMetaMember<D, T> implements MetaField<D, T> {
    private final Field field;

    DefaultMetaField(final MetaClass<D> declaringClass, final Field field) {
        super(declaringClass, field, DefaultMetaClass.newMetaClass(field.getGenericType(), TypeUtil.cast(field.getType())));
        this.field = field;
    }

    @Override
    public T get(final D target) {
        try {
            return TypeUtil.cast(field.get(target));
        } catch (final IllegalAccessException e) {
            throw new InitializationException("Error getting field value of " + field + " from target " + target, e);
        }
    }

    @Override
    public void set(final D target, final T value) {
        try {
            field.set(target, value);
        } catch (final IllegalAccessException e) {
            throw new InitializationException("Error setting field value of " + field + " on target " + target, e);
        }
    }

    @Override
    public String toString() {
        return field.toGenericString();
    }
}
