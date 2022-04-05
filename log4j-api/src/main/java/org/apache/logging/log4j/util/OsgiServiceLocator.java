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
package org.apache.logging.log4j.util;

import java.lang.invoke.MethodHandles.Lookup;
import java.util.stream.Stream;

import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class OsgiServiceLocator {

    private static final boolean OSGI_AVAILABLE = checkOsgiAvailable();

    private static boolean checkOsgiAvailable() {
        try {
            Class.forName("org.osgi.framework.Bundle");
            return true;
        } catch (final ClassNotFoundException | LinkageError e) {
            return false;
        } catch (final Throwable e) {
            LowLevelLogUtil.logException("Unknown error checking for existence of class: org.osgi.framework.Bundle", e);
            return false;
        }
    }

    public static boolean isAvailable() {
        return OSGI_AVAILABLE;
    }

    public static <T> Stream<T> loadServices(final Class<T> serviceType, final Lookup lookup) {
        return loadServices(serviceType, lookup, true);
    }

    public static <T> Stream<T> loadServices(final Class<T> serviceType, final Lookup lookup, final boolean verbose) {
        final Bundle bundle = FrameworkUtil.getBundle(lookup.lookupClass());
        if (bundle != null) {
            final BundleContext ctx = bundle.getBundleContext();
            try {
                return ctx.getServiceReferences(serviceType, null)
                        .stream()
                        .map(ctx::getService);
            } catch (Throwable e) {
                if (verbose) {
                    StatusLogger.getLogger().error("Unable to load OSGI services for service {}", serviceType, e);
                }
            }
        }
        return Stream.empty();
    }
}
