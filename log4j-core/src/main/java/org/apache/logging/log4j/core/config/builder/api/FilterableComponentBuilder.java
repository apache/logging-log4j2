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

/**
 * Component builder that can add Filters. Similar in idea to the {@link org.apache.logging.log4j.core.filter.Filterable}.
 *
 * @since 2.6
 */
public interface FilterableComponentBuilder<T extends ComponentBuilder<T>> extends ComponentBuilder<T> {

    /**
     * Adds a Filter to the component.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code FilterComponentBuilder} with all of its attributes and subcomponents set.
     * @return this component builder (for chaining)
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    T add(FilterComponentBuilder builder);
}
