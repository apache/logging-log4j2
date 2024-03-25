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

import java.lang.invoke.MethodHandles.Lookup;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleRevision;

public class OsgiServiceLocator {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final boolean OSGI_AVAILABLE = checkOsgiAvailable();

    private static boolean checkOsgiAvailable() {
        try {
            /*
             * OSGI classes of any version can still be present even if Log4j2 does not run in
             * an OSGI container, hence we check if this class is in a bundle.
             */
            final Class<?> clazz = Class.forName("org.osgi.framework.FrameworkUtil");
            return clazz.getMethod("getBundle", Class.class).invoke(null, OsgiServiceLocator.class) != null;
        } catch (final ClassNotFoundException | NoSuchMethodException | LinkageError e) {
            return false;
        } catch (final Throwable error) {
            LOGGER.error("Unknown error checking OSGI environment.", error);
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
        final Class<?> lookupClass = Objects.requireNonNull(lookup, "lookup").lookupClass();
        return loadServices(serviceType, lookupClass, StatusLogger.getLogger());
    }

    static <T> Stream<T> loadServices(final Class<T> serviceType, final Class<?> callerClass, final Logger logger) {
        final Bundle bundle = FrameworkUtil.getBundle(callerClass);
        if (bundle != null && !isFragment(bundle)) {
            final BundleContext ctx = bundle.getBundleContext();
            if (ctx == null) {
                logger.warn(
                        "Unable to load OSGi services for service {}: bundle {} (state {}) does not have a valid BundleContext",
                        serviceType::getName,
                        bundle::getSymbolicName,
                        () -> {
                            switch (bundle.getState()) {
                                case Bundle.UNINSTALLED:
                                    return "UNINSTALLED";
                                case Bundle.INSTALLED:
                                    return "INSTALLED";
                                case Bundle.RESOLVED:
                                    return "RESOLVED";
                                case Bundle.STARTING:
                                    return "STARTING";
                                case Bundle.STOPPING:
                                    return "STOPPING";
                                case Bundle.ACTIVE:
                                    return "ACTIVE";
                                default:
                                    return "UNKNOWN";
                            }
                        });

            } else {
                try {
                    return ctx.getServiceReferences(serviceType, null).stream().map(ctx::getService);
                } catch (final Exception e) {
                    logger.error("Unable to load OSGI services for service {}", serviceType, e);
                }
            }
        }
        return Stream.empty();
    }

    private static boolean isFragment(final Bundle bundle) {
        try {
            return (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
        } catch (final SecurityException ignored) {
            return false;
        }
    }
}
