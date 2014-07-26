/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * Bundle-based ResourceLoader.
 */
public final class BundleResourceLoader implements ResourceLoader {

    private final Bundle bundle;

    public BundleResourceLoader(final Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }

    @Override
    public URL getResource(final String name) {
        return bundle.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        final Enumeration<URL> enumeration = bundle.getResources(name);
        return enumeration;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + ": " + bundle.getSymbolicName();
    }
}
