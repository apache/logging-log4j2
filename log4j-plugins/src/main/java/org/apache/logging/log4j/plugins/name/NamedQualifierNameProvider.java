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

package org.apache.logging.log4j.plugins.name;

import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.util.Strings;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class NamedQualifierNameProvider implements AnnotatedElementNameProvider<Named>, AnnotatedElementAliasesProvider<Named> {
    @Override
    public Optional<String> getSpecifiedName(final Named annotation) {
        final String[] names = annotation.value();
        if (names == null || names.length == 0) {
            return Optional.empty();
        }
        return Strings.trimToOptional(names[0]);
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
