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

import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;

/**
 * Post-processor that registers {@link SystemPropertyBundle} and {@link DefaultBundle} for default bindings
 * used in Log4j.
 */
@Ordered(Ordered.LAST - 1000)
public class Log4jInstanceFactoryPostProcessor implements ConfigurableInstanceFactoryPostProcessor {
    @Override
    public void postProcessFactory(final ConfigurableInstanceFactory factory) {
        factory.registerBundles(ClockFactory.class, SystemPropertyBundle.class, DefaultBundle.class);
    }
}
