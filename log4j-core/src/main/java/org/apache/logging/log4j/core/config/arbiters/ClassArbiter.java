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

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Conditional that determines if the specified class is present.
 */
@Configurable(elementType = Arbiter.ELEMENT_TYPE, printObject = true, deferChildren = true)
@Plugin
public class ClassArbiter implements Arbiter {

    private final String className;

    private ClassArbiter(final String className) {
        this.className = className;
    }

    @Override
    public boolean isCondition() {
        return LoaderUtil.isClassAvailable(className);
    }

    @PluginFactory
    public static ClassArbiter.Builder newBuilder() {
        return new ClassArbiter.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<ClassArbiter> {

        @PluginBuilderAttribute
        private String className;

        public String getClassName() {
            return className;
        }

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

        @Override
        public ClassArbiter build() {
            return new ClassArbiter(className);
        }

    }
}
