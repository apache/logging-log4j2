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

import java.util.Collections;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;

/**
 * Interceptor to add a root object key to the event template.
 *
 * @see JsonTemplateLayout.Builder#getEventTemplateRootObjectKey()
 */
@Plugin(name = "EventRootObjectKeyInterceptor", category = TemplateResolverInterceptor.CATEGORY)
public class EventRootObjectKeyInterceptor implements EventResolverInterceptor {

    private static final EventRootObjectKeyInterceptor INSTANCE = new EventRootObjectKeyInterceptor();

    private EventRootObjectKeyInterceptor() {}

    @PluginFactory
    public static EventRootObjectKeyInterceptor getInstance() {
        return INSTANCE;
    }

    @Override
    public Object processTemplateBeforeResolverInjection(final EventResolverContext context, final Object node) {
        final String eventTemplateRootObjectKey = context.getEventTemplateRootObjectKey();
        return eventTemplateRootObjectKey != null ? Collections.singletonMap(eventTemplateRootObjectKey, node) : node;
    }
}
