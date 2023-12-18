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
package org.apache.logging.log4j.spi.recycler;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.logging.log4j.util.PropertyEnvironment;

/**
 * Contract for providing {@link RecyclerFactory} instances.
 *
 * @since 3.0.0
 */
public interface RecyclerFactoryProvider {

    /**
     * Denotes the value to be used while sorting recycler factory providers to determine the precedence order.
     * Values will be sorted naturally, that is, lower values will imply higher precedence.
     *
     * @return the value to be used while sorting
     */
    default int getOrder() {
        return 100;
    }

    /**
     * The name of this recycler factory provider.
     * Recycler factory providers are required to have unique names.
     *
     * @return the name of this recycler factory provider
     */
    String getName();

    /**
     * Creates a recycler factory for the provided environment.
     * <p>
     * The return value can be null indicating that the recycler factory is not available for the provided environment.
     * For instance, the provider of a {@link ThreadLocal}-based recycler factory can return null if the environment is of a web application.
     * </p>
     *
     * @param environment an environment
     * @return either a recycler factory instance, or null, if the associated recycler factory is not available for the given environment
     */
    @Nullable
    RecyclerFactory createForEnvironment(PropertyEnvironment environment);
}
