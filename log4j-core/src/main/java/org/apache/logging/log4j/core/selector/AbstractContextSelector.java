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
package org.apache.logging.log4j.core.selector;

import java.net.URI;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.jspecify.annotations.Nullable;

public abstract class AbstractContextSelector implements ContextSelector {

    protected final ConfigurableInstanceFactory instanceFactory;

    public AbstractContextSelector(final ConfigurableInstanceFactory instanceFactory) {
        this.instanceFactory = instanceFactory;
    }

    protected LoggerContext.Builder newBuilder() {
        return instanceFactory.getInstance(LoggerContext.Builder.class);
    }

    protected final LoggerContext createContext(
            final String contextName, final @Nullable URI configLocation, final ClassLoader loader) {
        return newBuilder()
                .setContextName(contextName)
                .setConfigLocation(configLocation)
                .setLoader(loader)
                .build();
    }
}
