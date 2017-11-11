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
package org.apache.logging.log4j.appserver.tomcat;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.juli.logging.Log;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Implements the Log interface from Tomcat 8.5 and greater.
 *
 * In order to use this class to cause Tomcat to use Log4j for logging, the jar containing this class as well as the
 * log4j-api and log4j-core jars must be added to Tomcat's boot classpath. This is most easily accomplished by
 * placing these jars in a directory and then adding the contents of that directory to the CLASSPATH
 * environment variable in setenv.sh in Tomcat's bin directory.
 *
 * The Log4j configuration file must also be present on the classpath. This implementation will use the
 * first file it finds with one of the following file names: log4j2-tomcat.xml, log4j2-tomcat.json,
 * log4j2-tomcat.yaml, log4j2-tomcat.yml, log4j2-tomcat.properties. Again, this can be accomplished by adding
 * this file to a directory and then adding that directory to the CLASSPATH environment variable in setenv.sh.
 *
 * @since 2.10.0
 */
public class TomcatLogger implements Log {

    private static final long serialVersionUID = 1L;
    private static final String FQCN = TomcatLogger.class.getName();
    private static final String[] FILE_NAMES = {
        "log4j2-tomcat.xml", "log4j2-tomcat.json", "log4j2-tomcat.yaml", "log4j2-tomcat.yml",
        "log4j2-tomcat.properties"
    };

    private final ExtendedLogger logger;

    /**
     * This constructor is used by ServiceLoader to load an instance of the class.
     */
    public TomcatLogger() {
        logger = null;
    }

    /**
     * This constructor is used by LogFactory to create a new Logger.
     * @param name The name of the Logger.
     */
    public TomcatLogger(final String name) {
        this.logger = PrivateManager.getLogger(name);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled() {
        return logger.isFatalEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void trace(final Object o) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, o, null);
    }

    @Override
    public void trace(final Object o, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, o, throwable);
    }

    @Override
    public void debug(final Object o) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, o, null);
    }

    @Override
    public void debug(final Object o, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, o, throwable);
    }

    @Override
    public void info(final Object o) {
        logger.logIfEnabled(FQCN, Level.INFO, null, o, null);
    }

    @Override
    public void info(final Object o, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.INFO, null, o, throwable);
    }

    @Override
    public void warn(final Object o) {
        logger.logIfEnabled(FQCN, Level.WARN, null, o, null);
    }

    @Override
    public void warn(final Object o, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.WARN, null, o, throwable);
    }

    @Override
    public void error(final Object o) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, o, null);
    }

    @Override
    public void error(final Object o, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, o, throwable);
    }

    @Override
    public void fatal(final Object o) {
        logger.logIfEnabled(FQCN, Level.FATAL, null, o, null);
    }

    @Override
    public void fatal(final Object o, final Throwable throwable) {
        logger.logIfEnabled(FQCN, Level.FATAL, null, o, throwable);
    }

    /**
     * Internal LogManager.
     */
    private static class PrivateManager extends LogManager {

        public static LoggerContext getContext() {
            final ClassLoader cl = TomcatLogger.class.getClassLoader();
            URI uri = null;
            for (final String fileName : FILE_NAMES) {
                try {
                    final URL url = cl.getResource(fileName);
                    if (url != null) {
                        uri = url.toURI();
                        break;
                    }
                } catch (final URISyntaxException ex) {
                    // Ignore the exception.
                }
            }
            if (uri == null) {
                return getContext(FQCN, cl, false);
            }
            return getContext(FQCN, cl, false, uri, "Tomcat");
        }

        public static ExtendedLogger getLogger(final String name) {
            return getContext().getLogger(name);
        }
    }
}
