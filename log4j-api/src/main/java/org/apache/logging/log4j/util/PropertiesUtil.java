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

package org.apache.logging.log4j.util;

/**
 * Provided for backward compatibility with plugins directly referencing this class.
 *
 * @deprecated use {@link PropertyEnvironment} for API and {@link org.apache.logging.log4j.util3.PropertiesUtil} for
 * internal usage.
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public class PropertiesUtil implements PropertyEnvironment {

    public static PropertiesUtil getProperties() {
        return new PropertiesUtil(org.apache.logging.log4j.util3.PropertiesUtil.getProperties());
    }
    private final PropertyEnvironment delegate;

    public PropertiesUtil(final PropertyEnvironment delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addPropertySource(final PropertySource propertySource) {
        delegate.addPropertySource(propertySource);
    }

    @Override
    public boolean hasProperty(final String name) {
        return delegate.hasProperty(name);
    }

    @Override
    public String getStringProperty(final String name) {
        return delegate.getStringProperty(name);
    }
}
