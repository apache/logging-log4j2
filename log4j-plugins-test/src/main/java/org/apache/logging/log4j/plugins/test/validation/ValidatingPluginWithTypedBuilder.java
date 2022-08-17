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
package org.apache.logging.log4j.plugins.test.validation;

import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

import java.util.Objects;

/**
 *
 */
@Namespace("Test")
@Plugin("ValidatingPluginWithTypedBuilder")
public class ValidatingPluginWithTypedBuilder {

    private final String name;

    public ValidatingPluginWithTypedBuilder(final String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    @PluginFactory
    public static Builder<Integer> newBuilder() {
        return new Builder<>();
    }

    public static class Builder<T> implements org.apache.logging.log4j.plugins.util.Builder<ValidatingPluginWithTypedBuilder> {

        @PluginBuilderAttribute
        @Required(message = "The name given by the builder is null")
        private String name;

        public Builder<T> setName(final String name) {
            this.name = name;
            return this;
        }

        @Override
        public ValidatingPluginWithTypedBuilder build() {
            return new ValidatingPluginWithTypedBuilder(name);
        }
    }
}
