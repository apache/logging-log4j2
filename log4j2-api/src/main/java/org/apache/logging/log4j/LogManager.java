package org.apache.logging.log4j;

import org.apache.logging.log4j.internal.StatusLogger;
import org.apache.logging.log4j.spi.LoggerContext;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * The anchor point for the logging system.
 */
public final class LogManager {

    private static final String LOGGER_RESOURCE = "META-INF/log4j-provider.xml";
    private static final String LOG_MANAGER_CLASS = "LoggerContextClass";
    private static final String API_VERSION = "Log4jAPIVersion";
    private static final String[] COMPATIBLE_API_VERSIONS = {
        "1.99.0"
    };

    private static LoggerContext manager;

    private static Logger logger = StatusLogger.getLogger();

    /**
     * Prevent instantiation
     */
    private LogManager() {
    }


    /**
     * Scans the classpath to find all logging implementation. Currently, only one will
     * be used but this could be extended to allow multiple implementations to be used.
     */
    static {
        ClassLoader cl = findClassLoader();
        List<LoggerContext> managers = new ArrayList<LoggerContext>();

        Enumeration enumResources = null;
        try {
            enumResources = cl.getResources(LOGGER_RESOURCE);
        }
        catch (IOException e) {
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
                String className = props.getProperty(LOG_MANAGER_CLASS);
                if (className != null) {
                    try {
                        Class clazz = Class.forName(className);
                        managers.add((LoggerContext) clazz.newInstance());
                    } catch (ClassNotFoundException cnfe) {
                        logger.error("Unable to locate class " + className + " specified in " + url.toString(), cnfe);
                    } catch (InstantiationException ie) {
                        logger.error("Unable to create class " + className + " specified in " + url.toString(), ie);
                    } catch (IllegalAccessException iae) {
                        logger.error("Unable to create class " + className + " specified in " + url.toString(), iae);
                    } catch (Exception e) {
                        logger.error("Unable to create class " + className + " specified in " + url.toString(), e);
                        e.printStackTrace();
                    }
                }
            }
            if (managers.size() != 1) {
                logger.fatal("Unable to locate a logging implementation");
            } else {
                manager = managers.get(0);
            }
        } else {
            logger.fatal("Unable to locate a logging implementation");
        }
    }

    /**
     * Return a Logger with the specified name.
     *
     * @param name The logger name.
     * @return The Logger.
     */
    public static Logger getLogger(String name) {
        return manager.getLogger(name);
    }

    public static LoggerContext getContext() {
        return manager;
    }

    private static ClassLoader findClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
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
