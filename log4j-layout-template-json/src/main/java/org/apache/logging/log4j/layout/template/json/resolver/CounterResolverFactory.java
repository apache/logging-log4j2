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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * {@link CounterResolver} factory.
 */
@Plugin(name = "CounterResolverFactory", category = TemplateResolverFactory.CATEGORY)
public final class CounterResolverFactory implements EventResolverFactory {

    private static final CounterResolverFactory INSTANCE = new CounterResolverFactory();

    private CounterResolverFactory() {}

    @PluginFactory
    public static CounterResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return CounterResolver.getName();
    }

    @Override
    public CounterResolver create(final EventResolverContext context, final TemplateResolverConfig config) {
        return new CounterResolver(context, config);
    }
}
