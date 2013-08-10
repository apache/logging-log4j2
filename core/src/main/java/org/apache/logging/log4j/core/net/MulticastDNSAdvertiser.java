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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.helpers.Integers;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Advertise an entity via ZeroConf/MulticastDNS and the JmDNS library.
 *
 * The length of property names and values must be 255 bytes or less.
 * Entries with names or values larger than 255 bytes will be removed prior to advertisement.
 *
 */
@Plugin(name = "multicastdns", category = "Core", elementType = "advertiser", printObject = false)
public class MulticastDNSAdvertiser implements Advertiser {
    protected static final Logger LOGGER = StatusLogger.getLogger();
    private static Object jmDNS = initializeJMDNS();

    private static Class<?> jmDNSClass;
    private static Class<?> serviceInfoClass;

    public MulticastDNSAdvertiser()
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
                jmDNSClass.getMethod("create", (Class[])null);
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
                final Method method = jmDNSClass.getMethod("registerService", new Class[]{serviceInfoClass});
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
        else
        {
            LOGGER.warn("JMDNS not available - will not advertise ZeroConf support");
            return null;
        }
    }

    /**
     * Unadvertise the previously advertised entity
     * @param serviceInfo
     */
    @Override
    public void unadvertise(final Object serviceInfo) {
        if (jmDNS != null) {
            try {
                final Method method = jmDNSClass.getMethod("unregisterService", new Class[]{serviceInfoClass});
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

    private static Object createJmDNSVersion1()
    {
        try {
            return jmDNSClass.newInstance();
        } catch (final InstantiationException e) {
            LOGGER.warn("Unable to instantiate JMDNS", e);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("Unable to instantiate JMDNS", e);
        }
        return null;
    }

    private static Object createJmDNSVersion3()
    {
        try {
            final Method jmDNSCreateMethod = jmDNSClass.getMethod("create", (Class[])null);
            return jmDNSCreateMethod.invoke(null, (Object[])null);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("Unable to instantiate jmdns class", e);
        } catch (final NoSuchMethodException e) {
            LOGGER.warn("Unable to access constructor", e);
        } catch (final InvocationTargetException e) {
            LOGGER.warn("Unable to call constructor", e);
        }
        return null;
    }

    private Object buildServiceInfoVersion1(final String zone, final int port, final String name, final Map<String, String> properties) {
        //version 1 uses a hashtable
        final Hashtable<String, String> hashtableProperties = new Hashtable<String, String>(properties);
        try {
            final Class<?>[] args = new Class<?>[6];
            args[0] = String.class;
            args[1] = String.class;
            args[2] = int.class;
            args[3] = int.class; //weight (0)
            args[4] = int.class; //priority (0)
            args[5] = Hashtable.class;
            final Constructor<?> constructor  = serviceInfoClass.getConstructor(args);
            final Object[] values = new Object[6];
            values[0] = zone;
            values[1] = name;
            values[2] = port;
            values[3] = 0;
            values[4] = 0;
            values[5] = hashtableProperties;
            return constructor.newInstance(values);
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

    private Object buildServiceInfoVersion3(final String zone, final int port, final String name, final Map<String, String> properties) {
        try {
            final Class<?>[] args = new Class<?>[6];
            args[0] = String.class; //zone/type
            args[1] = String.class; //display name
            args[2] = int.class; //port
            args[3] = int.class; //weight (0)
            args[4] = int.class; //priority (0)
            args[5] = Map.class;
            final Method serviceInfoCreateMethod = serviceInfoClass.getMethod("create", args);
            final Object[] values = new Object[6];
            values[0] = zone;
            values[1] = name;
            values[2] = port;
            values[3] = 0;
            values[4] = 0;
            values[5] = properties;
            return serviceInfoCreateMethod.invoke(null, values);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("Unable to invoke create method", e);
        } catch (final NoSuchMethodException e) {
            LOGGER.warn("Unable to find create method", e);
        } catch (final InvocationTargetException e) {
            LOGGER.warn("Unable to invoke create method", e);
        }
        return null;
    }

    private static Object initializeJMDNS() {
        try {
            jmDNSClass = Class.forName("javax.jmdns.JmDNS");
            serviceInfoClass = Class.forName("javax.jmdns.ServiceInfo");
            //if version 3 is available, use it to constuct a serviceInfo instance, otherwise support the version1 API
            boolean isVersion3 = false;
            try {
                //create method is in version 3, not version 1
                jmDNSClass.getMethod("create", (Class[])null);
                isVersion3 = true;
            } catch (final NoSuchMethodException e) {
                //no-op
            }

            if (isVersion3) {
                return createJmDNSVersion3();
            } else {
                return createJmDNSVersion1();
            }
        } catch (final ClassNotFoundException e) {
            LOGGER.warn("JmDNS or serviceInfo class not found", e);
        } catch (final ExceptionInInitializerError e2) {
            LOGGER.warn("JmDNS or serviceInfo class not found", e2);
        }
        return null;
    }
}
