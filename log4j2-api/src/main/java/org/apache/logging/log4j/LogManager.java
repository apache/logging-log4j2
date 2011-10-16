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
package org.apache.logging.log4j;

import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * The anchor point for the logging system.
 */
public class LogManager {

    public static final String ROOT_LOGGER_NAME = "";

    private static final String LOGGER_RESOURCE = "META-INF/log4j-provider.xml";
    private static final String LOGGER_CONTEXT_FACTORY = "LoggerContextFactory";
    private static final String API_VERSION = "Log4jAPIVersion";
    private static final String[] COMPATIBLE_API_VERSIONS = {
        "1.99.0"
    };

    private static LoggerContextFactory factory;

    private static Logger logger = StatusLogger.getLogger();

    /**
     * Prevent instantiation
     */
    protected LogManager() {
    }


    /**
     * Scans the classpath to find all logging implementation. Currently, only one will
     * be used but this could be extended to allow multiple implementations to be used.
     */
    static {
        ClassLoader cl = findClassLoader();
        List<LoggerContextFactory> factories = new ArrayList<LoggerContextFactory>();

        Enumeration enumResources = null;
        try {
            enumResources = cl.getResources(LOGGER_RESOURCE);
        } catch (IOException e) {
            logger.fatal("Unable to locate " + LOGGER_RESOURCE, e);
        }

        if (enumResources != null) {
            while (enumResources.hasMoreElements()) {
                Properties props = new Properties();
                URL url = (URL) enumResources.nextElement();
                try {
                    props.loadFromXML(url.openStream());
                } catch (IOException ioe) {
                    logger.error("Unable to read " + url.toString(), ioe);
                }
                if (!validVersion(props.getProperty(API_VERSION))) {
                    continue;
                }
                String className = props.getProperty(LOGGER_CONTEXT_FACTORY);
                if (className != null) {
                    try {
                        Class clazz = cl.loadClass(className);
                        if (LoggerContextFactory.class.isAssignableFrom(clazz)) {
                            factories.add((LoggerContextFactory) clazz.newInstance());
                        } else {
                            logger.error(className + " does not implement " + LoggerContextFactory.class.getName());
                        }
                    } catch (ClassNotFoundException cnfe) {
                        logger.error("Unable to locate class " + className + " specified in " + url.toString(), cnfe);
                    } catch (IllegalAccessException iae) {
                        logger.error("Unable to create class " + className + " specified in " + url.toString(), iae);
                    } catch (Exception e) {
                        logger.error("Unable to create class " + className + " specified in " + url.toString(), e);
                        e.printStackTrace();
                    }
                }
            }
            if (factories.size() != 1) {
                logger.fatal("Unable to locate a logging implementation");
            } else {
                factory = factories.get(0);
            }
        } else {
            logger.fatal("Unable to locate a logging implementation");
        }
    }

    /**
     * Return the LoggerContextFactory.
     * @return The LoggerContextFactory.
     */
    public static LoggerContextFactory getFactory() {
        return factory;
    }

    /**
     * Return a Logger with the specified name.
     *
     * @param name The logger name.
     * @return The Logger.
     */
    public static Logger getLogger(String name) {

        return factory.getContext(LogManager.class.getName(), false).getLogger(name);
    }

    /**
     * Return a Logger with the specified name.
     *
     * @param fqcn The fully qualified class name of the class that this method is a member of.
     * @param name The logger name.
     * @return The Logger.
     */
    protected static Logger getLogger(String fqcn, String name) {

        return factory.getContext(fqcn, false).getLogger(name);
    }

    /**
     * Returns the current LoggerContext.
     * <p>
     * WARNING - The LoggerContext returned by this method may not be the LoggerContext used to create a Logger
     * for the calling class.
     * @return  The current LoggerContext.
     */
    public static LoggerContext getContext() {
        return factory.getContext(LogManager.class.getName(), true);
    }

    /**
     * Returns a LoggerContext.
     *
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(boolean currentContext) {
        return factory.getContext(LogManager.class.getName(), currentContext);
    }

    /**
     * Returns a LoggerContext
     * @param fqcn The fully qualified class name of the Class that this method is a member of.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    protected static LoggerContext getContext(String fqcn, boolean currentContext) {
        return factory.getContext(fqcn, currentContext);
    }

    private static ClassLoader findClassLoader() {
        ClassLoader cl;
        if (System.getSecurityManager() == null) {
            cl = Thread.currentThread().getContextClassLoader();
        } else {
            cl = (ClassLoader) java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                }
            );
        }
        if (cl == null) {
            cl = LogManager.class.getClassLoader();
        }

        return cl;
    }

    private static boolean validVersion(String version) {
        for (String v : COMPATIBLE_API_VERSIONS) {
            if (version.startsWith(v)) {
                return true;
            }
        }
        return false;
    }



}
