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
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Conditional that determines if the specified class is present.
 */
@Plugin(
        name = "ClassArbiter",
        category = Node.CATEGORY,
        elementType = Arbiter.ELEMENT_TYPE,
        printObject = true,
        deferChildren = true)
public class ClassArbiter implements Arbiter {

    private final String className;

    private ClassArbiter(final String className) {
        this.className = className;
    }

    @Override
    public boolean isCondition() {
        return LoaderUtil.isClassAvailable(className);
    }

    @PluginBuilderFactory
    public static ClassArbiter.Builder newBuilder() {
        return new ClassArbiter.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ClassArbiter> {

        public static final String ATTR_CLASS_NAME = "className";

        @PluginBuilderAttribute(ATTR_CLASS_NAME)
        private String className;

        /**
         * Sets the Class name.
         * @param className the class name.
         * @return this
         */
        public Builder setClassName(final String className) {
            this.className = className;
            return asBuilder();
        }

        public Builder asBuilder() {
            return this;
        }

        public ClassArbiter build() {
            return new ClassArbiter(className);
        }
    }
}
