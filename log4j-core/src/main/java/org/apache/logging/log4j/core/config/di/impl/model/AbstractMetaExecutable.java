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

import org.apache.logging.log4j.core.config.di.api.model.MetaClass;
import org.apache.logging.log4j.core.config.di.api.model.MetaExecutable;
import org.apache.logging.log4j.core.config.di.api.model.MetaParameter;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractMetaExecutable<D, T> extends AbstractMetaMember<D, T> implements MetaExecutable<D> {
    private final List<MetaParameter> parameters;

    AbstractMetaExecutable(final MetaClass<D> declaringClass, final Executable executable, final MetaClass<T> type) {
        super(declaringClass, executable, type);
        parameters = new ArrayList<>(executable.getParameterCount());
        for (final Parameter parameter : executable.getParameters()) {
            parameters.add(new DefaultMetaParameter(parameter));
        }
    }

    @Override
    public List<MetaParameter> getParameters() {
        return parameters;
    }
}
