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
package org.apache.logging.log4j.kit.recycler;

import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.recycler.internal.DummyRecyclerFactoryProvider;

/**
 * Contract for providing {@link RecyclerFactory} instances.
 *
 * @since 3.0.0
 */
public interface RecyclerFactoryProvider {

    static RecyclerFactoryProvider getInstance() {
        return DummyRecyclerFactoryProvider.INSTANCE;
    }

    /**
     * Denotes the value to be used while sorting recycler factory providers to determine the precedence order.
     * Values will be sorted naturally, that is, lower values will imply higher precedence.
     *
     * @return the value to be used while sorting
     */
    default int getOrder() {
        return 0;
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
     *
     * @param environment an environment
     * @return a recycler factory instance for the given environment
     */
    RecyclerFactory createForEnvironment(PropertyEnvironment environment);
}
