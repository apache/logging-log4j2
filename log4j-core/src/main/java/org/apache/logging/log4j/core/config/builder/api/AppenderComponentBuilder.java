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

import org.apache.logging.log4j.core.Appender;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A builder interface for constructing and configuring {@link Appender} components in a Log4j configuration.
 *
 * <p>
 *   Instances of this builder are designed for single-threaded use and are not thread-safe. Developers
 *   should avoid sharing instances between threads.
 * </p>
 *
 * @since 2.4
 */
@ProviderType
public interface AppenderComponentBuilder extends FilterableComponentBuilder<AppenderComponentBuilder> {

    /**
     * Adds a {@link LayoutComponentBuilder} to this Appender component builder.
     * <p>
     *   Note: the provided {@code builder} will be built by this method; therefore, it must be fully configured
     *   <i>before</i> calling this method.  Changes to the builder after calling this method will not have
     *   any effect.
     * </p>
     *
     * @param builder The {@code LayoutComponentBuilder} with all of its attributes set.
     * @return this component builder (for chaining)
     * @throws NullPointerException if the given {@code builder} argument is {@code null}
     */
    AppenderComponentBuilder add(LayoutComponentBuilder builder);
}
