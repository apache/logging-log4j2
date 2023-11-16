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
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class OsgiServiceLocator {

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
        } catch (final Throwable e) {
            LowLevelLogUtil.logException("Unknown error checking OSGI environment.", e);
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
        final Bundle bundle = FrameworkUtil.getBundle(Objects.requireNonNull(lookupClass, "lookupClass"));
        if (bundle != null) {
            final BundleContext ctx = bundle.getBundleContext();
            if (ctx == null) {
                if (verbose) {
                    StatusLogger.getLogger()
                            .error(
                                    "Unable to load OSGI services: The bundle has no valid BundleContext for serviceType = {}, lookup = {}, lookupClass = {}, bundle = {}",
                                    serviceType,
                                    lookup,
                                    lookupClass,
                                    bundle);
                }
            } else {
                try {
                    return ctx.getServiceReferences(serviceType, null).stream().map(ctx::getService);
                } catch (Throwable e) {
                    if (verbose) {
                        StatusLogger.getLogger().error("Unable to load OSGI services for service {}", serviceType, e);
                    }
                }
            }
        }
        return Stream.empty();
    }
}
