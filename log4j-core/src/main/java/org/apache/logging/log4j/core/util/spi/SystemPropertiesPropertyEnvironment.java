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
package org.apache.logging.log4j.core.util.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.config.spi.PropertyEnvironmentSPI;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * {@link PropertyEnvironmentSPI} adapter backed by Log4j system properties.
 */
public final class SystemPropertiesPropertyEnvironment implements PropertyEnvironmentSPI {

    private final PropertiesUtil propertiesUtil;

    public SystemPropertiesPropertyEnvironment() {
        this(PropertiesUtil.getProperties());
    }

    SystemPropertiesPropertyEnvironment(final PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    @Override
    public String getProperty(final String key) {
        return propertiesUtil.getStringProperty(key, null);
    }

    @Override
    public Map<String, String> getProperties() {
        final Properties systemProperties = System.getProperties();
        final Map<String, String> map = new HashMap<>(systemProperties.size());
        for (final String name : systemProperties.stringPropertyNames()) {
            map.put(name, systemProperties.getProperty(name));
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean containsProperty(final String key) {
        return propertiesUtil.hasProperty(key);
    }
}
