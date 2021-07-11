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

package org.apache.logging.log4j.plugins.di.spi;

import java.util.List;
import java.util.Map;

public abstract class BeanInfoService {
    private final List<String> injectableClassNames;
    private final List<String> producibleClassNames;
    private final List<String> destructibleClassNames;
    private final Map<String, List<String>> pluginCategories;

    protected BeanInfoService(final List<String> injectableClassNames, final List<String> producibleClassNames,
                              final List<String> destructibleClassNames, final Map<String, List<String>> pluginCategories) {
        this.injectableClassNames = injectableClassNames;
        this.producibleClassNames = producibleClassNames;
        this.destructibleClassNames = destructibleClassNames;
        this.pluginCategories = pluginCategories;
    }

    public List<String> getInjectableClassNames() {
        return injectableClassNames;
    }

    public List<String> getProducibleClassNames() {
        return producibleClassNames;
    }

    public List<String> getDestructibleClassNames() {
        return destructibleClassNames;
    }

    public Map<String, List<String>> getPluginCategories() {
        return pluginCategories;
    }
}
