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

package org.apache.logging.log4j.appserver.jetty;

import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Logger;

/**
 * Provides a native Apache Log4j 2 for Jetty logging.
 */
public class Log4j2Logger extends AbstractLogger {

    private final org.apache.logging.log4j.Logger logger;

    private final String name;

    public Log4j2Logger(final String name) {
        super();
        this.name = name;
        this.logger = LogManager.getLogger(name);
    }

    public Log4j2Logger() {
        this("");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#debug(java.lang.String, java.lang.Object[])
     */
    @Override
    public void debug(final String msg, final Object... args) {
        logger.debug(msg, args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#debug(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void debug(final String msg, final Throwable thrown) {
        logger.debug(msg, thrown);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#debug(java.lang.Throwable)
     */
    @Override
    public void debug(final Throwable thrown) {
        logger.debug(thrown);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#ignore(java.lang.Throwable)
     */
    @Override
    public void ignore(final Throwable ignored) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#info(java.lang.String, java.lang.Object[])
     */
    @Override
    public void info(final String msg, final Object... args) {
        logger.info(msg, args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#info(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void info(final String msg, final Throwable thrown) {
        logger.info(msg, thrown);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#info(java.lang.Throwable)
     */
    @Override
    public void info(final Throwable thrown) {
        logger.info(thrown);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#isDebugEnabled()
     */
    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.AbstractLogger#newLogger(java.lang.String)
     */
    @Override
    protected Logger newLogger(final String fullname) {
        return new Log4j2Logger(fullname);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#setDebugEnabled(boolean)
     */
    @Override
    public void setDebugEnabled(final boolean enabled) {
        warn("setDebugEnabled not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#warn(java.lang.String, java.lang.Object[])
     */
    @Override
    public void warn(final String msg, final Object... args) {
        logger.warn(msg, args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#warn(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void warn(final String msg, final Throwable thrown) {
        logger.warn(msg, thrown);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jetty.util.log.Logger#warn(java.lang.Throwable)
     */
    @Override
    public void warn(final Throwable thrown) {
        logger.warn(thrown);
    }

}
