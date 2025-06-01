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
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.LoggerConfig.RootLogger;
import org.jspecify.annotations.Nullable;

/**
 * A common builder interface for constructing and configuring {@link Logger} and {@link RootLogger}
 * components in a Log4j configuration.
 *
 * <p>
 *   Instances of this builder are designed for single-threaded use and are not thread-safe. Developers
 *   should avoid sharing instances between threads.
 * </p>
 *
 * @since 2.6
 */
public interface LoggableComponentBuilder<T extends ComponentBuilder<T>> extends FilterableComponentBuilder<T> {

    /**
     * Add an {@link AppenderRefComponentBuilder} to this logger component builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code AppenderRefComponentBuilder} with all of its attributes and subcomponents set.
     * @return this component builder (for chaining)
     * @throws NullPointerException if the given {@code builder} argument is {@code null}
     */
    T add(AppenderRefComponentBuilder builder);

    /**
     * Sets the "{@code additivity}" attribute on the logger component.
     *
     * @param additivity {@code true} if additive; otherwise, {@code false}
     * @return this builder (for chaining)
     */
    default T setAdditivityAttribute(boolean additivity) {
        return setAttribute("additivity", additivity);
    }

    /**
     * Sets the "{@code additivity}" attribute on the logger component.
     * <p>
     *   If the given {@code additivity} is {@code null}, the attribute will be removed from the component.
     * </p>
     *
     * @param additivity "{@code true}" if additive; otherwise, {@code false}
     * @return this builder (for chaining)
     */
    default T setAdditivityAttribute(String additivity) {
        return setAttribute("additivity", additivity);
    }

    /**
     * Sets the "{@code includeLocation}" attribute on the loggable component.
     * <p>
     *   If the given {@code includeLocation} is {@code null}, the attribute will be removed from the component.
     * </p>
     *
     * @param includeLocation {@code true} to include location information; otherwise, {@code false}
     * @return this builder (for chaining)
     */
    default T setIncludeLocationAttribute(boolean includeLocation) {
        return setAttribute("includeLocation", includeLocation);
    }

    /**
     * Sets the "{@code level}" attribute on the loggable component.
     * <p>
     *   If the given {@code level} is {@code null}, the attribute will be removed from the component.
     * </p>
     *
     * @param level the level
     * @return this builder (for chaining)
     */
    default T setLevelAttribute(@Nullable String level) {
        return setAttribute("level", level);
    }

    /**
     * Sets the "{@code level}" attribute on the loggable component.
     * <p>
     *   If the given {@code level} is {@code null}, the attribute will be removed from the component.
     * </p>
     *
     * @param level the level
     * @return this builder (for chaining)
     */
    default T setLevelAttribute(@Nullable Level level) {
        return setAttribute("level", level);
    }
}
