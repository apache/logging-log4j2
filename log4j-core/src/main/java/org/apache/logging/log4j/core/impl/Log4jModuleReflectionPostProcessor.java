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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.apache.logging.log4j.plugins.di.spi.ReflectionAgent;

/**
 * Post-processor that registers a {@link ReflectionAgent} using {@code log4j-core} as the calling context.
 * This makes it so that plugins that require an open module can open themselves to a common module.
 */
@Ordered(Ordered.FIRST + 100)
public class Log4jModuleReflectionPostProcessor implements ConfigurableInstanceFactoryPostProcessor {
    @Override
    public void postProcessFactory(final ConfigurableInstanceFactory factory) {
        factory.setReflectionAgent(object -> object.setAccessible(true));
    }
}
