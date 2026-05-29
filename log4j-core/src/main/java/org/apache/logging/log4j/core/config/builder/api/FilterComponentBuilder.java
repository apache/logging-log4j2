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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.jspecify.annotations.Nullable;

/**
 * A builder interface for constructing and configuring {@link Filter} components in a Log4j configuration.
 *
 * <p>
 *   Instances of this builder are designed for single-threaded use and are not thread-safe. Developers
 *   should avoid sharing instances between threads.
 * </p>
 *
 * @since 2.4
 */
public interface FilterComponentBuilder extends ComponentBuilder<FilterComponentBuilder> {

    /**
     * Sets the 'onMatch' attribute on the filter component.
     * <p>
     *   If the given {@code onMatch} argument is {@code} the attribute will be removed (if present).
     * </p>
     *
     * @param onMatch the attribute value
     * @return this builder (for chaining)
     */
    default FilterComponentBuilder setOnMatchAttribute(@Nullable String onMatch) {
        return setAttribute("onMatch", onMatch);
    }

    /**
     * Sets the 'onMatch' attribute on the filter component.
     * <p>
     *   If the given {@code onMatch} argument is {@code} the attribute will be removed (if present).
     * </p>
     *
     * @param onMatch the attribute value
     * @return this builder (for chaining)
     */
    default FilterComponentBuilder setOnMatchAttribute(@Nullable Result onMatch) {
        return setAttribute("onMatch", onMatch);
    }

    /**
     * Sets the 'onMismatch' attribute on the filter component.
     * <p>
     *   If the given {@code onMismatch} argument is {@code} the attribute will be removed (if present).
     * </p>
     *
     * @param onMismatch the attribute value
     * @return this builder (for chaining)
     */
    default FilterComponentBuilder setOnMismatchAttribute(@Nullable String onMismatch) {
        return setAttribute("onMismatch", onMismatch);
    }

    /**
     * Sets the 'onMismatch' attribute on the filter component.
     * <p>
     *   If the given {@code onMismatch} argument is {@code} the attribute will be removed (if present).
     * </p>
     *
     * @param onMismatch the attribute value
     * @return this builder (for chaining)
     */
    default FilterComponentBuilder setOnMismatchAttribute(@Nullable Result onMismatch) {
        return setAttribute("onMismatch", onMismatch);
    }
}
