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
package org.apache.logging.log4j.core.config.plugins.validation;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(name = "PluginWithGenericSubclassFoo1Builder", category = "Test")
public class PluginWithGenericSubclassFoo1Builder extends AbstractPluginWithGenericBuilder {

    public static class Builder<B extends Builder<B>> extends AbstractPluginWithGenericBuilder.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<PluginWithGenericSubclassFoo1Builder> {

        @PluginBuilderFactory
        public static <B extends Builder<B>> B newBuilder() {
            return new Builder<B>().asBuilder();
        }

        @PluginBuilderAttribute
        @Required(message = "The foo1 given by the builder is null")
        private String foo1;

        @Override
        public PluginWithGenericSubclassFoo1Builder build() {
            return new PluginWithGenericSubclassFoo1Builder(getThing(), getFoo1());
        }

        public String getFoo1() {
            return foo1;
        }

        public B withFoo1(final String foo1) {
            this.foo1 = foo1;
            return asBuilder();
        }

    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String foo1;

    public PluginWithGenericSubclassFoo1Builder(final String thing, final String foo1) {
        super(thing);
        this.foo1 = foo1;
    }

    public String getFoo1() {
        return foo1;
    }

}
