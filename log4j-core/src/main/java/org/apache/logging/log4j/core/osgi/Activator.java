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
package org.apache.logging.log4j.core.osgi;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * OSGi BundleActivator.
 */
@Header(name = org.osgi.framework.Constants.BUNDLE_ACTIVATOR, value = "${@class}")
@Header(name = org.osgi.framework.Constants.BUNDLE_ACTIVATIONPOLICY, value = org.osgi.framework.Constants.ACTIVATION_LAZY)
public final class Activator implements BundleActivator {
    private final AtomicReference<BundleContext> contextRef = new AtomicReference<>();
    private OsgiBundlePostProcessor bundlePostProcessor;

    @Override
    public void start(final BundleContext context) throws Exception {
        bundlePostProcessor = new OsgiBundlePostProcessor(context);
        contextRef.compareAndSet(null, context);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        bundlePostProcessor.close();
        this.contextRef.compareAndSet(context, null);
        LogManager.shutdown(false, true);
    }
}
