/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.config.builder.api;

/**
 * Gathers information in preparation for creating components. These assemblers are primarily useful for
 * aggregating the information that will be used to create a Configuration.
 *
 * @param <T> the Component class this is a builder for.
 */
public interface Assembler<T> {

    /**
     * Builds the plugin object after all configuration has been set. This will use default values for any
     * unspecified attributes for the plugin.
     *
     * @return the configured plugin instance.
     * @throws org.apache.logging.log4j.core.config.ConfigurationException if there was an error building the plugin
     * object.
     */
    T assemble();
}
