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
package org.apache.logging.log4j.core.config.plugins.util;

import java.lang.reflect.Type;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.resolver.AbstractAttributeFactoryResolver;
import org.apache.logging.log4j.plugins.di.spi.StringValueResolver;
import org.jspecify.annotations.Nullable;

@Deprecated(since = "3.0", forRemoval = true)
@SuppressWarnings("removal")
public class LegacyPluginBuilderAttributeFactoryResolver<T>
        extends AbstractAttributeFactoryResolver<T, PluginBuilderAttribute> {
    public LegacyPluginBuilderAttributeFactoryResolver() {
        super(PluginBuilderAttribute.class);
    }

    @Override
    protected boolean isSensitive(final PluginBuilderAttribute annotation) {
        return annotation.sensitive();
    }

    @Override
    protected @Nullable T getDefaultValue(
            final PluginBuilderAttribute annotation,
            final StringValueResolver resolver,
            final Type type,
            final TypeConverter<T> typeConverter) {
        return null;
    }
}
