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

import org.apache.logging.log4j.plugins.Category;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class Keys {
    private Keys() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SUBSTITUTOR_NAME = "StringSubstitutor";
    public static final Key<Function<String, String>> SUBSTITUTOR_KEY = new @Named(SUBSTITUTOR_NAME) Key<>() {};

    public static final String PLUGIN_PACKAGES_NAME = "PluginPackages";
    public static final Key<List<String>> PLUGIN_PACKAGES_KEY = new @Named(PLUGIN_PACKAGES_NAME) Key<>() {};

    public static String getCategory(final AnnotatedElement element) {
        return Optional.ofNullable(element.getAnnotation(Category.class))
                .map(Category::value)
                .orElseGet(() -> Stream.of(element.getAnnotations())
                        .map(annotation -> annotation.annotationType().getAnnotation(Category.class))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(Category::value)
                        .orElse(Strings.EMPTY));
    }
}
