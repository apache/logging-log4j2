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

package org.apache.logging.log4j.core.config.plugins.visitors;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.inject.AbstractConfigurationInjector;
import org.apache.logging.log4j.util.NameUtil;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * @deprecated Provided for support for PluginBuilderAttribute.
 */
// copy of PluginBuilderAttributeInjector
public class PluginBuilderAttributeVisitor extends AbstractConfigurationInjector<PluginBuilderAttribute, Configuration> {
    @Override
    public Object inject(final Object target) {
        return findAndRemoveNodeAttribute()
                .map(stringSubstitutionStrategy)
                .map(value -> {
                    String debugValue = annotation.sensitive() ? NameUtil.md5(value + getClass().getName()) : value;
                    StringBuilders.appendKeyDqValue(debugLog, name, debugValue);
                    return optionBinder.bindString(target, value);
                })
                .orElseGet(() -> optionBinder.bindObject(target, null));
    }
}