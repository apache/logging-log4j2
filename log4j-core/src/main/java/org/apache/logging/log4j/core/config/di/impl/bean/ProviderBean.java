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

package org.apache.logging.log4j.core.config.di.impl.bean;

import org.apache.logging.log4j.plugins.api.Provider;
import org.apache.logging.log4j.core.config.di.api.bean.InitializationContext;
import org.apache.logging.log4j.core.config.di.api.bean.ProviderFactory;
import org.apache.logging.log4j.core.config.di.api.model.Variable;

class ProviderBean<T> extends SystemBean<Provider<T>> {
    private final ProviderFactory<T> providerFactory;

    ProviderBean(final Variable variable, final ProviderFactory<T> providerFactory) {
        super(variable);
        this.providerFactory = providerFactory;
    }

    @Override
    public Provider<T> create(final InitializationContext<Provider<T>> context) {
        return providerFactory.getProvider(context);
    }
}
