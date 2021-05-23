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

/**
 * Main {@link TemplateResolver} compilation interception interface.
 *
 * @param <V> type of the value passed to the {@link TemplateResolver resolver}
 * @param <C> type of the context employed
 */
public interface TemplateResolverInterceptor<V, C extends TemplateResolverContext<V, C>> {

    /**
     * Main plugin category for {@link TemplateResolverInterceptor} implementations.
     */
    String CATEGORY = "JsonTemplateResolverInterceptor";

    /**
     * The targeted value class.
     */
    Class<V> getValueClass();

    /**
     * The targeted {@link TemplateResolverContext} class.
     */
    Class<C> getContextClass();

    /**
     * Intercept the read template before compiler (i.e.,
     * {@link TemplateResolvers#ofTemplate(TemplateResolverContext, String)}
     * starts injecting resolvers.
     * <p>
     * This is the right place to introduce, say, contextual additional fields.
     *
     * @param node the root object of the read template
     * @return the root object of the template to be compiled
     */
    default Object processTemplateBeforeResolverInjection(C context, Object node) {
        return node;
    }

}
