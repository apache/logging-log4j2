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

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.config.spi.PropertyEnvironmentSPI;

/**
 * Chains multiple {@link PropertyEnvironmentSPI} instances with first-match precedence.
 */
public final class ChainedPropertyEnvironment implements PropertyEnvironmentSPI {

    private final PropertyEnvironmentSPI[] environments;

    public ChainedPropertyEnvironment(final PropertyEnvironmentSPI... environments) {
        this.environments = environments.clone();
    }

    @Override
    public String getProperty(final String key) {
        for (final PropertyEnvironmentSPI environment : environments) {
            final String value = environment.getProperty(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        final Map<String, String> merged = new LinkedHashMap<>();
        for (int i = environments.length - 1; i >= 0; i--) {
            merged.putAll(environments[i].getProperties());
        }
        return merged;
    }

    @Override
    public boolean containsProperty(final String key) {
        for (final PropertyEnvironmentSPI environment : environments) {
            if (environment.containsProperty(key)) {
                return true;
            }
        }
        return false;
    }
}
