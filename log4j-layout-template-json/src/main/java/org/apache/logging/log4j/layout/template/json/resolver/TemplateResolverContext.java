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

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Context used to compile a template and passed to
 * {@link TemplateResolverFactory#create(TemplateResolverContext, TemplateResolverConfig)
 * template resolver factory creator}s.
 *
 * @param <V> type of the value passed to the resolver as input
 * @param <C> type of the context passed to the {@link TemplateResolverFactory resolver factory}
 *
 * @see TemplateResolverFactory
 */
interface TemplateResolverContext<V, C extends TemplateResolverContext<V, C>> {

    Class<C> getContextClass();

    Map<String, ? extends TemplateResolverFactory<V, C>> getResolverFactoryByName();

    List<? extends TemplateResolverInterceptor<V, C>> getResolverInterceptors();

    TemplateResolverStringSubstitutor<V> getSubstitutor();

    JsonWriter getJsonWriter();

    /**
     * Process the read template before compiler (i.e.,
     * {@link TemplateResolvers#ofTemplate(TemplateResolverContext, String)}
     * starts injecting resolvers.
     * <p>
     * This is the right place to introduce, say, contextual additional fields.
     *
     * @param node the root object of the read template
     * @return the root object of the template to be compiled
     */
    default Object processTemplateBeforeResolverInjection(Object node) {
        return node;
    }
}
