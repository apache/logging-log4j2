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
package org.apache.log4j.jmx;

import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Manages an instance of com.sun.jdmk.comm.HtmlAdapterServer which was provided for demonstration purposes in the Java
 * Management Extensions Reference Implementation 1.2.1. This class is provided to maintain compatibility with earlier
 * versions of log4j and use in new code is discouraged.
 *
 * @deprecated
 */
@Deprecated
public class Agent {

    /**
     * Diagnostic logger.
     *
     * @deprecated
     */
    @Deprecated
    static Logger log = Logger.getLogger(Agent.class);

    /**
     * Creates a new instance of com.sun.jdmk.comm.HtmlAdapterServer using reflection.
     *
     * @since 1.2.16
     * @return new instance.
     */
    private static Object createServer() {
        Object newInstance = null;
        try {
            newInstance = LoaderUtil.newInstanceOf("com.sun.jdmk.comm.HtmlAdapterServer");
        } catch (final ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return newInstance;
    }

    /**
     * Invokes HtmlAdapterServer.start() using reflection.
     *
     * @since 1.2.16
     * @param server instance of com.sun.jdmk.comm.HtmlAdapterServer.
     */
    private static void startServer(final Object server) {
        try {
            server.getClass().getMethod("start", new Class[0]).invoke(server, new Object[0]);
        } catch (final InvocationTargetException ex) {
            final Throwable cause = ex.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause != null) {
                if (cause instanceof InterruptedException || cause instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException(cause.toString());
            } else {
                throw new RuntimeException();
            }
        } catch (final NoSuchMethodException ex) {
            throw new RuntimeException(ex.toString());
        } catch (final IllegalAccessException ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * Create new instance.
     *
     * @deprecated
     */
    @Deprecated
    public Agent() {}

    /**
     * Starts instance of HtmlAdapterServer.
     *
     * @deprecated
     */
    @Deprecated
    public void start() {

        final MBeanServer server = MBeanServerFactory.createMBeanServer();
        final Object html = createServer();

        try {
            log.info("Registering HtmlAdaptorServer instance.");
            server.registerMBean(html, new ObjectName("Adaptor:name=html,port=8082"));
            log.info("Registering HierarchyDynamicMBean instance.");
            final HierarchyDynamicMBean hdm = new HierarchyDynamicMBean();
            server.registerMBean(hdm, new ObjectName("log4j:hiearchy=default"));
        } catch (final JMException e) {
            log.error("Problem while registering MBeans instances.", e);
            return;
        } catch (final RuntimeException e) {
            log.error("Problem while registering MBeans instances.", e);
            return;
        }
        startServer(html);
    }
}
