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
package org.apache.logging.log4j.plugins.di.resolver;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginValue;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.spi.FactoryResolver;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.di.spi.StringValueResolver;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Factory resolver for {@link PluginValue}-annotated keys. This injects a configurable plugin value. Plugin
 * values are similar to attributes but can be specified as a value node in configuration formats that make such
 * a distinction like XML.
 */
public class PluginValueFactoryResolver implements FactoryResolver<String> {
    private final Class<? extends Annotation> annotationType;

    public PluginValueFactoryResolver() {
        this(PluginValue.class);
    }

    protected PluginValueFactoryResolver(final Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean supportsKey(final Key<?> key) {
        return key.getQualifierType() == annotationType;
    }

    @Override
    public Supplier<String> getFactory(
            final ResolvableKey<String> resolvableKey, final InstanceFactory instanceFactory) {
        return () -> {
            final Node node = instanceFactory.getInstance(Node.CURRENT_NODE);
            final String name = resolvableKey.getKey().getName();
            final String nodeValue = node.getValue();
            final Optional<String> attributeValue = node.removeMatchingAttribute(name, resolvableKey.getAliases())
                    .filter(Strings::isNotEmpty);
            final String rawValue;
            if (Strings.isNotEmpty(nodeValue)) {
                attributeValue.ifPresent(attribute -> StatusLogger.getLogger()
                        .error(
                                "Configuration contains {} with both attribute value ({}) AND element value ({}). "
                                        + "Please specify only one value. Using the element value.",
                                node.getName(),
                                attribute,
                                nodeValue));
                rawValue = nodeValue;
            } else {
                rawValue = attributeValue.orElse(null);
            }
            if (Strings.isEmpty(rawValue) || !instanceFactory.hasBinding(StringValueResolver.KEY)) {
                return rawValue;
            }
            final StringValueResolver resolver = instanceFactory.getInstance(StringValueResolver.class);
            return resolver.resolve(rawValue);
        };
    }
}
