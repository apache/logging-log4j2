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

import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 *
 */
public class AbstractPluginWithGenericBuilder {

    public static abstract class Builder<B extends Builder<B>> {

        @PluginBuilderAttribute
        @Required(message = "The thing given by the builder is null")
        private String thing;

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }

        public String getThing() {
            return thing;
        }

        public B withThing(final String name) {
            this.thing = name;
            return asBuilder();
        }

    }

    private final String thing;

    public AbstractPluginWithGenericBuilder(final String thing) {
        super();
        this.thing = thing;
    }

    public String getThing() {
        return thing;
    }
}
