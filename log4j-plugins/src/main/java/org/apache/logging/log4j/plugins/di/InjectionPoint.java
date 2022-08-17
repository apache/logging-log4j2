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

package org.apache.logging.log4j.plugins.di;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InjectionPoint<T> {
    private final Key<T> key;
    private final Collection<String> aliases;
    private final Member member;
    private final AnnotatedElement element;

    InjectionPoint(final Key<T> key, final Collection<String> aliases, final Member member, final AnnotatedElement element) {
        this.key = key;
        this.aliases = aliases;
        this.member = member;
        this.element = element;
    }

    public Key<T> getKey() {
        return key;
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public Member getMember() {
        return member;
    }

    public AnnotatedElement getElement() {
        return element;
    }

    static <T> InjectionPoint<T> forField(final Field field) {
        final Key<T> key = Key.forField(field);
        final Collection<String> aliases = Keys.getAliases(field);
        return new InjectionPoint<>(key, aliases, field, field);
    }

    static <T> InjectionPoint<T> forParameter(final Executable executable, final Parameter parameter) {
        final Key<T> key = Key.forParameter(parameter);
        final Collection<String> aliases = Keys.getAliases(parameter);
        return new InjectionPoint<>(key, aliases, executable, parameter);
    }

    static List<InjectionPoint<?>> fromExecutable(final Executable executable) {
        return Stream.of(executable.getParameters())
                .map(parameter -> forParameter(executable, parameter))
                .collect(Collectors.toList());
    }
}
