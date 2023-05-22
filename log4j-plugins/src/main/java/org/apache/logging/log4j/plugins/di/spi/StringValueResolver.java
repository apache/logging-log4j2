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
package org.apache.logging.log4j.plugins.di.spi;

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Key;

/**
 * Strategy for replacing strings containing variable placeholders with their resolved values. This is used
 * when parsing {@link Node} attributes and values before injecting them into qualified injection points.
 *
 * @see org.apache.logging.log4j.plugins.PluginAttribute
 * @see org.apache.logging.log4j.plugins.PluginBuilderAttribute
 * @see org.apache.logging.log4j.plugins.PluginValue
 */
@FunctionalInterface
public interface StringValueResolver {
    Key<StringValueResolver> KEY = new Key<>() {};

    StringValueResolver NOOP = s -> s;

    /**
     * Resolves the provided input string by potentially replacing variable placeholders.
     *
     * @param input input string to resolve values inside
     * @return the resolved string
     */
    String resolve(final String input);
}
