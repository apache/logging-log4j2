/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.plugins.di.spi;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.Keys;

public record InjectionPoint<T>(Key<T> key, Collection<String> aliases, AnnotatedElement element) {

    public static final Key<InjectionPoint<?>> CURRENT_INJECTION_POINT = new Key<>() {};

    public static <T> InjectionPoint<T> forField(final Field field) {
        final Key<T> key = Key.forField(field);
        final Collection<String> aliases = Keys.getAliases(field);
        return new InjectionPoint<>(key, aliases, field);
    }

    public static <T> InjectionPoint<T> forParameter(final Parameter parameter) {
        final Key<T> key = Key.forParameter(parameter);
        final Collection<String> aliases = Keys.getAliases(parameter);
        return new InjectionPoint<>(key, aliases, parameter);
    }

    public static List<InjectionPoint<?>> fromExecutable(final Executable executable) {
        return Stream.of(executable.getParameters())
                .map(InjectionPoint::forParameter)
                .collect(Collectors.toList());
    }
}
