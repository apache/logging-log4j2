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
package org.apache.logging.log4j.core.config.arbiters;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * Condition that determines if the specified property is set.
 */
@Plugin(
        name = "SystemPropertyArbiter",
        category = Node.CATEGORY,
        elementType = Arbiter.ELEMENT_TYPE,
        deferChildren = true,
        printObject = true)
public class SystemPropertyArbiter implements Arbiter {

    private final String propertyName;
    private final String propertyValue;

    private SystemPropertyArbiter(final String propertyName, final String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    /**
     * Returns true if either the property name is defined (it has any value) or the property value
     * matches the requested value.
     */
    @Override
    public boolean isCondition() {
        final String value = System.getProperty(propertyName);
        return value != null && (propertyValue == null || value.equals(propertyValue));
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<SystemPropertyArbiter> {

        public static final String ATTR_PROPERTY_NAME = "propertyName";
        public static final String ATTR_PROPERTY_VALUE = "propertyValue";

        @PluginBuilderAttribute(ATTR_PROPERTY_NAME)
        private String propertyName;

        @PluginBuilderAttribute(ATTR_PROPERTY_VALUE)
        private String propertyValue;
        /**
         * Sets the Property Name.
         * @param propertyName the property name.
         * @return this
         */
        public Builder setPropertyName(final String propertyName) {
            this.propertyName = propertyName;
            return asBuilder();
        }

        /**
         * Sets the Property Value.
         * @param propertyValue the property value.
         * @return this
         */
        public Builder setPropertyValue(final String propertyValue) {
            this.propertyValue = propertyValue;
            return asBuilder();
        }

        public Builder asBuilder() {
            return this;
        }

        public SystemPropertyArbiter build() {
            return new SystemPropertyArbiter(propertyName, propertyValue);
        }
    }
}
