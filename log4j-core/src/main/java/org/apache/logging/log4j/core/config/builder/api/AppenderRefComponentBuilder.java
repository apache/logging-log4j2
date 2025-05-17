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
package org.apache.logging.log4j.core.config.builder.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.jspecify.annotations.Nullable;

/**
 * A builder interface for constructing and configuring {@link AppenderRef} components in a Log4j configuration.
 *
 * <p>
 *   Instances of this builder are designed for single-threaded use and are not thread-safe. Developers
 *   should avoid sharing instances between threads.
 * </p>
 *
 * @since 2.4
 */
public interface AppenderRefComponentBuilder extends FilterableComponentBuilder<AppenderRefComponentBuilder> {

    /**
     * Sets the "{@code level}" attribute on the appender-reference component.
     * <p>
     *   If the given {@code level} is {@code null}, the attribute will be removed from the component.
     * </p>
     *
     * @param level the level
     * @return this builder (for chaining)
     */
    default AppenderRefComponentBuilder setLevelAttribute(@Nullable String level) {
        return setAttribute("level", level);
    }

    /**
     * Sets the "{@code level}" attribute on the appender reference component.
     * <p>
     *   If the given {@code level} is {@code null}, the attribute will be removed from the component.
     * </p>
     *
     * @param level the level
     * @return this builder (for chaining)
     */
    default AppenderRefComponentBuilder setLevelAttribute(@Nullable Level level) {
        return setAttribute("level", level);
    }

    /**
     * Sets the "{@code ref}" attribute on the appender reference component.
     * <p>
     *   If the given {@code refAppenderName} is {@code null}, the attribute will be removed from the component.
     * </p>
     *
     * @param refAppenderName the name of the appender being referenced
     * @return this builder (for chaining)
     */
    default AppenderRefComponentBuilder setRefAttribute(@Nullable String refAppenderName) {
        return setAttribute("ref", refAppenderName);
    }
}
