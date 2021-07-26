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

package org.apache.logging.log4j.plugins.di.model;

import java.lang.annotation.Annotation;
import java.util.Set;

public class ProducerField implements PluginSource {
    private final String declaringClassName;
    private final String fieldName;
    private final Set<Class<?>> implementedInterfaces;
    private final Class<? extends Annotation> scopeType;

    public ProducerField(final String declaringClassName, final String fieldName,
                         final Set<Class<?>> implementedInterfaces, final Class<? extends Annotation> scopeType) {
        this.declaringClassName = declaringClassName;
        this.fieldName = fieldName;
        this.implementedInterfaces = implementedInterfaces;
        this.scopeType = scopeType;
    }

    @Override
    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Set<Class<?>> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        return scopeType;
    }
}
