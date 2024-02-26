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
package org.apache.logging.log4j.util;

import java.util.Hashtable;
import org.apache.logging.log4j.spi.Provider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Utility class to register Log4j2 providers in an OSGI environment.
 */
public abstract class ProviderActivator implements BundleActivator {

    public static final String API_VERSION = "APIVersion";

    private final Provider provider;
    private ServiceRegistration<Provider> providerRegistration = null;

    protected ProviderActivator(final Provider provider) {
        this.provider = provider;
    }

    @Override
    @SuppressWarnings("JdkObsolete")
    public void start(final BundleContext context) throws Exception {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(API_VERSION, provider.getVersions());
        providerRegistration = context.registerService(Provider.class, provider, props);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (providerRegistration != null) {
            providerRegistration.unregister();
        }
    }
}
