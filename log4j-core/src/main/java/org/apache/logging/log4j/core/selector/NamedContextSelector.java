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
package org.apache.logging.log4j.core.selector;

import java.net.URI;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * ContextSelectors that have a name.
 */
public interface NamedContextSelector extends ContextSelector {

    /**
     * Locate the LoggerContext with the specified name.
     * @param name The LoggerContext name.
     * @param externalContext The external context to associate with the LoggerContext.
     * @param configLocation The location of the configuration.
     * @return A LoggerContext.
     */
    LoggerContext locateContext(String name, Object externalContext, URI configLocation);

    /**
     * Locate the LoggerContext with the specified name using the default configuration.
     * @param name The LoggerContext name.
     * @return A LoggerContext.
     */
    LoggerContext removeContext(String name);
}
