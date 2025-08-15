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
package example;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

@Configurable
@Plugin("PluginWithGenericSubclassFoo1Builder")
public class PluginWithGenericSubclassFoo1Builder extends AbstractPluginWithGenericBuilder {

    public static class Builder<B extends Builder<B>> extends AbstractPluginWithGenericBuilder.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<PluginWithGenericSubclassFoo1Builder> {

        @PluginAttribute
        @Required(message = "The foo1 given by the builder is null")
        private String foo1;

        @Override
        public PluginWithGenericSubclassFoo1Builder build() {
            return new PluginWithGenericSubclassFoo1Builder(getThing(), getFoo1());
        }

        public String getFoo1() {
            return foo1;
        }

        public B setFoo1(final String foo1) {
            this.foo1 = foo1;
            return asBuilder();
        }
    }

    @PluginFactory
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
