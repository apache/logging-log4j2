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

package org.apache.logging.log4j.core.config.plugins.visit;

import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Keys;

import java.lang.reflect.AnnotatedElement;
import java.util.function.Function;

public class PluginBuilderAttributeVisitor extends org.apache.logging.log4j.plugins.visit.PluginBuilderAttributeVisitor {
    @Inject
    public PluginBuilderAttributeVisitor(
            @Named(Keys.SUBSTITUTOR_NAME) final Function<String, String> stringSubstitutionStrategy,
            final Injector injector) {
        super(stringSubstitutionStrategy, injector);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean isSensitive(final AnnotatedElement element) {
        return element.getAnnotation(PluginBuilderAttribute.class).sensitive();
    }
}
