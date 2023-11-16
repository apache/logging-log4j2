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

/**
 * {@link TemplateResolver} factory.
 *
 * @param <V> type of the value passed to the {@link TemplateResolver resolver}
 * @param <C> type of the context passed to the {@link TemplateResolverFactory#create(TemplateResolverContext, TemplateResolverConfig)} creator}
 */
public interface TemplateResolverFactory<V, C extends TemplateResolverContext<V, C>> {

    /**
     * Main plugin category for {@link TemplateResolverFactory} implementations.
     */
    String CATEGORY = "JsonTemplateResolverFactory";

    /**
     * The targeted value class.
     */
    Class<V> getValueClass();

    /**
     * The targeted {@link TemplateResolverContext} class.
     */
    Class<C> getContextClass();

    String getName();

    TemplateResolver<V> create(C context, TemplateResolverConfig config);
}
