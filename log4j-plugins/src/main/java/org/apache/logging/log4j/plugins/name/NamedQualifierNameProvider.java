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
package org.apache.logging.log4j.plugins.name;

import static java.util.Optional.ofNullable;
import static org.apache.logging.log4j.util.Strings.trimToNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.plugins.Named;

public class NamedQualifierNameProvider
        implements AnnotatedElementNameProvider<Named>, AnnotatedElementAliasesProvider<Named> {
    @Override
    public Optional<String> getSpecifiedName(final Named annotation) {
        final String[] names = annotation.value();
        if (names == null || names.length == 0) {
            return Optional.empty();
        }
        return ofNullable(trimToNull(names[0]));
    }

    @Override
    public Collection<String> getAliases(final Named annotation) {
        final String[] names = annotation.value();
        if (names == null || names.length <= 1) {
            return List.of();
        }
        final String[] aliases = new String[names.length - 1];
        for (int i = 0; i < aliases.length; i++) {
            aliases[i] = names[i + 1].trim();
        }
        return List.of(aliases);
    }
}
