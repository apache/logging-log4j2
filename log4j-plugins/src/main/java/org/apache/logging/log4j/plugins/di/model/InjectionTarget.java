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

import org.apache.logging.log4j.plugins.util.PluginUtil;
import org.apache.logging.log4j.plugins.util.Value;

import java.util.Set;

public class InjectionTarget implements PluginSource {
    private final Value<Class<?>> declaringClass;
    private final Set<Class<?>> implementedInterfaces;

    public InjectionTarget(
            final ClassLoader classLoader, final String className, final Set<Class<?>> implementedInterfaces) {
        declaringClass = PluginUtil.lazyLoadClass(classLoader, className);
        this.implementedInterfaces = implementedInterfaces;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass.get();
    }

    @Override
    public Set<Class<?>> getImplementedInterfaces() {
        return implementedInterfaces;
    }
}
