/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.logging.log4j.core.net;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Advertise an entity via ZeroConf/MulticastDNS and the JmDNS library.
 *
 * The length of property names and values must be 255 bytes or less.
 * Entries with names or values larger than 255 bytes will be removed prior to advertisement.
 *
 */
@Plugin(name = "multicastdns", category = "Core", elementType = "advertiser", printObject = false)
public class MulticastDnsAdvertiser implements Advertiser {
    protected static final Logger LOGGER = StatusLogger.getLogger();
    private static Object jmDNS = initializeJmDns();

    private static Class<?> jmDNSClass;
    private static Class<?> serviceInfoClass;

    public MulticastDnsAdvertiser()
    {
        //no arg constructor for reflection
    }

    /**
     * Advertise the provided entity.
     *
     * Properties map provided in advertise method must include a "name" entry
     * but may also provide "protocol" (tcp/udp) as well as a "port" entry
     *
     * The length of property names and values must be 255 bytes or less.
     * Entries with names or values larger than 255 bytes will be removed prior to advertisement.
     *
     * @param properties the properties representing the entity to advertise
     * @return the object which can be used to unadvertise, or null if advertisement was unsuccessful
     */
    @Override
    public Object advertise(final Map<String, String> properties) {
        //default to tcp if "protocol" was not set
        final Map<String, String> truncatedProperties = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry:properties.entrySet())
        {
            if (entry.getKey().length() <= 255 && entry.getValue().length() <= 255)
            {
                truncatedProperties.put(entry.getKey(), entry.getValue());
            }
        }
        final String protocol = truncatedProperties.get("protocol");
        final String zone = "._log4j._"+(protocol != null ? protocol : "tcp") + ".local.";
        //default to 4555 if "port" was not set
        final String portString = truncatedProperties.get("port");
        final int port = Integers.parseInt(portString, 4555);

        final String name = truncatedProperties.get("name");

        //if version 3 is available, use it to construct a serviceInfo instance, otherwise support the version1 API
        if (jmDNS != null)
        {
            boolean isVersion3 = false;
            try {
                //create method is in version 3, not version 1
                jmDNSClass.getMethod("create");
                isVersion3 = true;
            } catch (final NoSuchMethodException e) {
                //no-op
            }
            Object serviceInfo;
            if (isVersion3) {
                serviceInfo = buildServiceInfoVersion3(zone, port, name, truncatedProperties);
            } else {
                serviceInfo = buildServiceInfoVersion1(zone, port, name, truncatedProperties);
            }

            try {
                final Method method = jmDNSClass.getMethod("registerService", serviceInfoClass);
                method.invoke(jmDNS, serviceInfo);
            } catch(final IllegalAccessException e) {
                LOGGER.warn("Unable to invoke registerService method", e);
            } catch(final NoSuchMethodException e) {
                LOGGER.warn("No registerService method", e);
            } catch(final InvocationTargetException e) {
                LOGGER.warn("Unable to invoke registerService method", e);
            }
            return serviceInfo;
        }
        LOGGER.warn("JMDNS not available - will not advertise ZeroConf support");
        return null;
    }

    /**
     * Unadvertise the previously advertised entity
     * @param serviceInfo
     */
    @Override
    public void unadvertise(final Object serviceInfo) {
        if (jmDNS != null) {
            try {
                final Method method = jmDNSClass.getMethod("unregisterService", serviceInfoClass);
                method.invoke(jmDNS, serviceInfo);
            } catch(final IllegalAccessException e) {
                LOGGER.warn("Unable to invoke unregisterService method", e);
            } catch(final NoSuchMethodException e) {
                LOGGER.warn("No unregisterService method", e);
            } catch(final InvocationTargetException e) {
                LOGGER.warn("Unable to invoke unregisterService method", e);
            }
        }
    }

    private static Object createJmDnsVersion1()
    {
        try {
            return jmDNSClass.getConstructor().newInstance();
        } catch (final InstantiationException e) {
            LOGGER.warn("Unable to instantiate JMDNS", e);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("Unable to instantiate JMDNS", e);
        } catch (final NoSuchMethodException e) {
            LOGGER.warn("Unable to instantiate JMDNS", e);
        } catch (final InvocationTargetException e) {
            LOGGER.warn("Unable to instantiate JMDNS", e);
        }
        return null;
    }

    private static Object createJmDnsVersion3()
    {
        try {
            final Method jmDNSCreateMethod = jmDNSClass.getMethod("create");
            return jmDNSCreateMethod.invoke(null, (Object[])null);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("Unable to invoke create method", e);
        } catch (final NoSuchMethodException e) {
            LOGGER.warn("Unable to get create method", e);
        } catch (final InvocationTargetException e) {
            LOGGER.warn("Unable to invoke create method", e);
        }
        return null;
    }

    private static Object buildServiceInfoVersion1(final String zone,
                                                   final int port,
                                                   final String name,
                                                   final Map<String, String> properties) {
        //version 1 uses a hashtable
        @SuppressWarnings("UseOfObsoleteCollectionType")
        final Hashtable<String, String> hashtableProperties = new Hashtable<String, String>(properties);
        try {
            return serviceInfoClass
                    .getConstructor(String.class, String.class, int.class, int.class, int.class, Hashtable.class)
                    .newInstance(zone, name, port, 0, 0, hashtableProperties);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("Unable to construct ServiceInfo instance", e);
        } catch (final NoSuchMethodException e) {
            LOGGER.warn("Unable to get ServiceInfo constructor", e);
        } catch (final InstantiationException e) {
            LOGGER.warn("Unable to construct ServiceInfo instance", e);
        } catch (final InvocationTargetException e) {
            LOGGER.warn("Unable to construct ServiceInfo instance", e);
        }
        return null;
    }

    private static Object buildServiceInfoVersion3(final String zone,
                                                   final int port,
                                                   final String name,
                                                   final Map<String, String> properties) {
        try {
            return serviceInfoClass //   zone/type     display name  port       weight     priority   properties
                    .getMethod("create", String.class, String.class, int.class, int.class, int.class, Map.class)
                    .invoke(null, zone, name, port, 0, 0, properties);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("Unable to invoke create method", e);
        } catch (final NoSuchMethodException e) {
            LOGGER.warn("Unable to find create method", e);
        } catch (final InvocationTargetException e) {
            LOGGER.warn("Unable to invoke create method", e);
        }
        return null;
    }

    private static Object initializeJmDns() {
        try {
            jmDNSClass = Loader.loadClass("javax.jmdns.JmDNS");
            serviceInfoClass = Loader.loadClass("javax.jmdns.ServiceInfo");
            //if version 3 is available, use it to constuct a serviceInfo instance, otherwise support the version1 API
            boolean isVersion3 = false;
            try {
                //create method is in version 3, not version 1
                jmDNSClass.getMethod("create");
                isVersion3 = true;
            } catch (final NoSuchMethodException e) {
                //no-op
            }

            if (isVersion3) {
                return createJmDnsVersion3();
            }
            return createJmDnsVersion1();
        } catch (final ClassNotFoundException e) {
            LOGGER.warn("JmDNS or serviceInfo class not found", e);
        } catch (final ExceptionInInitializerError e2) {
            LOGGER.warn("JmDNS or serviceInfo class not found", e2);
        }
        return null;
    }
}
