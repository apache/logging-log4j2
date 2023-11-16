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
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * Default Condition for a Select Condition.
 */
@Plugin(
        name = "DefaultArbiter",
        category = Node.CATEGORY,
        elementType = Arbiter.ELEMENT_TYPE,
        deferChildren = true,
        printObject = true)
public class DefaultArbiter implements Arbiter {

    /**
     * Always returns true since it is the default.
     */
    @Override
    public boolean isCondition() {
        return true;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<DefaultArbiter> {

        public Builder asBuilder() {
            return this;
        }

        public DefaultArbiter build() {
            return new DefaultArbiter();
        }
    }
}
