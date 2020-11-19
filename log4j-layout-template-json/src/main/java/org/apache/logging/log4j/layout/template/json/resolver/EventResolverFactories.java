/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EventResolverFactories {

    private EventResolverFactories() {}

    private static final Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> RESOLVER_FACTORY_BY_NAME =
            createResolverFactoryByName();

    private static Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> createResolverFactoryByName() {

        // Collect resolver factories.
        final List<EventResolverFactory<? extends EventResolver>> resolverFactories = Arrays.asList(
                ThreadContextDataResolverFactory.getInstance(),
                ThreadContextStackResolverFactory.getInstance(),
                EndOfBatchResolverFactory.getInstance(),
                ExceptionResolverFactory.getInstance(),
                ExceptionRootCauseResolverFactory.getInstance(),
                LevelResolverFactory.getInstance(),
                LoggerResolverFactory.getInstance(),
                MainMapResolverFactory.getInstance(),
                MapResolverFactory.getInstance(),
                MarkerResolverFactory.getInstance(),
                MessageResolverFactory.getInstance(),
                MessageParameterResolverFactory.getInstance(),
                PatternResolverFactory.getInstance(),
                SourceResolverFactory.getInstance(),
                ThreadResolverFactory.getInstance(),
                TimestampResolverFactory.getInstance());

        // Convert collection to map.
        final Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> resolverFactoryByName = new LinkedHashMap<>();
        for (final EventResolverFactory<? extends EventResolver> resolverFactory : resolverFactories) {
            resolverFactoryByName.put(resolverFactory.getName(), resolverFactory);
        }
        return Collections.unmodifiableMap(resolverFactoryByName);

    }

    static Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> getResolverFactoryByName() {
        return RESOLVER_FACTORY_BY_NAME;
    }

}
