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

package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.util.StringBuilders;

import java.util.Optional;

public class PluginBuilderAttributeInjector extends AbstractConfigurationInjector<PluginBuilderAttribute, Object> {
    @Override
    public void inject(final Object factory) {
        final Optional<String> value = findAndRemoveNodeAttribute().map(stringSubstitutionStrategy);
        if (value.isPresent()) {
            final String str = value.get();
            debugLog(str);
            configurationBinder.bindString(factory, str);
        } else {
            debugLog.append(name).append("=null");
            configurationBinder.bindObject(factory, null);
        }
    }

    private void debugLog(final Object value) {
        final Object debugValue = annotation.sensitive() ? "*****" : value;
        StringBuilders.appendKeyDqValue(debugLog, name, debugValue);
    }
}
