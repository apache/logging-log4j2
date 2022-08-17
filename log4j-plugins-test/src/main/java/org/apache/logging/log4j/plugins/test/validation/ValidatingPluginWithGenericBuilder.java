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
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

import java.util.Objects;

/**
 *
 */
@Namespace("Test")
@Plugin("ValidatingPluginWithGenericBuilder")
public class ValidatingPluginWithGenericBuilder {

    private final String name;

    public ValidatingPluginWithGenericBuilder(final String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    @PluginFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static class Builder<B extends Builder<B>> implements org.apache.logging.log4j.plugins.util.Builder<ValidatingPluginWithGenericBuilder> {

        @PluginAttribute
        @Required(message = "The name given by the builder is null")
        private String name;

        public B setName(final String name) {
            this.name = name;
            return asBuilder();
        }

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }

        @Override
        public ValidatingPluginWithGenericBuilder build() {
            return new ValidatingPluginWithGenericBuilder(name);
        }
    }
}
