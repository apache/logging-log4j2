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
package org.apache.logging.log4j.plugins.visit;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

public class PluginValueVisitor implements NodeVisitor {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Function<String, String> stringSubstitutionStrategy;

    @Inject
    public PluginValueVisitor(@Named(Keys.SUBSTITUTOR_NAME) final Function<String, String> stringSubstitutionStrategy) {
        this.stringSubstitutionStrategy = stringSubstitutionStrategy;
    }

    private String parseValue(
            final Node node, final String name, final Collection<String> aliases, final StringBuilder debugLog) {
        final String elementValue = node.getValue();
        final Optional<String> attributeValue = node.removeMatchingAttribute(name, aliases).filter(Strings::isNotEmpty);
        final String rawValue;
        if (Strings.isNotEmpty(elementValue)) {
            attributeValue.ifPresent(value -> LOGGER.error(
                    "Configuration contains {} with both attribute value ({}) AND element value ({}). " +
                            "Please specify only one value. Using the element value.",
                    node.getName(), value, elementValue));
            rawValue = elementValue;
        } else {
            rawValue = attributeValue.orElse(null);
        }
        final String value = stringSubstitutionStrategy.apply(rawValue);
        StringBuilders.appendKeyDqValueWithJoiner(debugLog, "value", value, ", ");
        return value;
    }

    @Override
    public Object visitField(final Field field, final Node node, final StringBuilder debugLog) {
        final String name = Keys.getName(field);
        final Collection<String> aliases = Keys.getAliases(field);
        return Cast.cast(parseValue(node, name, aliases, debugLog));
    }

    @Override
    public Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog) {
        final String name = Keys.getName(parameter);
        final Collection<String> aliases = Keys.getAliases(parameter);
        return Cast.cast(parseValue(node, name, aliases, debugLog));
    }
}
