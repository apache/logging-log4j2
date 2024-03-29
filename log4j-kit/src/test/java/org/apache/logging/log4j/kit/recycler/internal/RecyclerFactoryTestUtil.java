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
package org.apache.logging.log4j.kit.recycler.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.TestPropertyEnvironment;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.kit.recycler.RecyclerFactoryProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.jspecify.annotations.Nullable;

final class RecyclerFactoryTestUtil {

    private RecyclerFactoryTestUtil() {}

    static @Nullable RecyclerFactory createForEnvironment(final String factory, final @Nullable Integer capacity) {
        final Map<String, String> properties = new HashMap<>();
        properties.put("recycler.factory", factory);
        if (capacity != null) {
            properties.put("recycler.capacity", capacity.toString());
        }
        final PropertyEnvironment env = new TestPropertyEnvironment(properties);
        return ServiceLoaderUtil.safeStream(
                        RecyclerFactoryProvider.class,
                        ServiceLoader.load(
                                RecyclerFactoryProvider.class, RecyclerFactoryTestUtil.class.getClassLoader()),
                        StatusLogger.getLogger())
                .filter(p -> factory.equals(p.getName()))
                .findFirst()
                .map(p -> p.createForEnvironment(env))
                .orElse(null);
    }
}
