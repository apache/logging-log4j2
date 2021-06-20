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

import org.apache.logging.log4j.plugins.di.Named;
import org.apache.logging.log4j.plugins.di.NamedAliases;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NamedAliasesProvider implements AnnotatedElementNameProvider<NamedAliases>, AnnotatedElementAliasesProvider<NamedAliases> {
    @Override
    public Optional<String> getSpecifiedName(final NamedAliases annotation) {
        final Named[] named = annotation.value();
        if (named.length == 0) {
            return Optional.empty();
        }
        return Strings.trimToOptional(named[0].value());
    }

    @Override
    public Collection<String> getAliases(final NamedAliases annotation) {
        // first @Named is the primary name
        final Named[] named = annotation.value();
        final int size = named.length - 1;
        if (size <= 0) {
            return Collections.emptyList();
        }
        final List<String> aliases = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            aliases.add(named[i + 1].value());
        }
        return aliases;
    }
}
