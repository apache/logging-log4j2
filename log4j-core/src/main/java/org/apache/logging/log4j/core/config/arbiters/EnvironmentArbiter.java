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
package org.apache.logging.log4j.core.config.arbiters;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * Condition that determines if the specified environment variable is set.
 */
@Plugin(name = "EnvironmentArbiter", category = Node.CATEGORY, elementType = Arbiter.ELEMENT_TYPE,
        deferChildren = true, printObject = true)
public class EnvironmentArbiter implements Arbiter {

    private final String variableName;
    private final String variableValue;

    private EnvironmentArbiter(final String variableName, final String variableValue) {
        this.variableName = variableName;
        this.variableValue = variableValue;
    }


    /**
     * Returns true if either the environment variable is defined (it has any value) or the property value
     * matches the requested value.
     */
    @Override
    public boolean isCondition() {
        String value = System.getenv(variableName);
        return value != null && (variableValue == null || value.equals(variableValue));
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<EnvironmentArbiter> {

        public static final String ATTR_VARIABLE_NAME = "variableName";
        public static final String ATTR_VARIABLE_VALUE = "variableValue";

        @PluginBuilderAttribute(ATTR_VARIABLE_NAME)
        private String variableName;

        @PluginBuilderAttribute(ATTR_VARIABLE_VALUE)
        private String variableValue;
        /**
         * Sets the Property Name.
         * @param variableName the property name.
         * @return this
         */
        public Builder setVariableName(final String variableName) {
            this.variableName = variableName;
            return asBuilder();
        }

        /**
         * Sets the Property Value.
         * @param variableValue the property name.
         * @return this
         */
        public Builder setVariableValue(final String variableValue) {
            this.variableValue = variableValue;
            return asBuilder();
        }

        public Builder asBuilder() {
            return this;
        }

        public EnvironmentArbiter build() {
            return new EnvironmentArbiter(variableName, variableValue);
        }
    }
}
