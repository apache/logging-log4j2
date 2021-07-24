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

import java.util.Map;
import java.util.Set;

public abstract class PluginModule {
    private final Map<String, Set<String>> injectionBeans;
    private final Map<String, Set<String>> producerBeans;
    private final Set<String> destructorBeans;
    private final Map<String, Set<String>> pluginBeans;

    protected PluginModule(final Map<String, Set<String>> injectionBeans,
                           final Map<String, Set<String>> producerBeans,
                           final Set<String> destructorBeans,
                           final Map<String, Set<String>> pluginBeans) {
        this.injectionBeans = injectionBeans;
        this.producerBeans = producerBeans;
        this.destructorBeans = destructorBeans;
        this.pluginBeans = pluginBeans;
    }

    public Map<String, Set<String>> getInjectionBeans() {
        return injectionBeans;
    }

    public Map<String, Set<String>> getProducerBeans() {
        return producerBeans;
    }

    public Set<String> getDestructorBeans() {
        return destructorBeans;
    }

    public Map<String, Set<String>> getPluginBeans() {
        return pluginBeans;
    }
}
