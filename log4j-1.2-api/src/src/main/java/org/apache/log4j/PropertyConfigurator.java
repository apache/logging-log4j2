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
package org.apache.log4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.spi.LoggerRepository;

/**
 * A configurator for properties.
 */
public class PropertyConfigurator {

    /**
     * Read configuration options from configuration file.
     *
     * @param configFileName The configuration file
     * @param hierarchy The hierarchy
     */
    public void doConfigure(final String configFileName, final LoggerRepository hierarchy) {

    }

    /**
     * Read configuration options from <code>properties</code>.
     *
     * See {@link #doConfigure(String, LoggerRepository)} for the expected format.
     *
     * @param properties The properties
     * @param hierarchy The hierarchy
     */
    public void doConfigure(final Properties properties, final LoggerRepository hierarchy) {
    }

    /**
     * Read configuration options from an InputStream.
     *
     * @param inputStream The input stream
     * @param hierarchy The hierarchy
     */
    public void doConfigure(final InputStream inputStream, final LoggerRepository hierarchy) {
    }

    /**
     * Read configuration options from url <code>configURL</code>.
     *
     * @param configURL The configuration URL
     * @param hierarchy The hierarchy
     */
    public void doConfigure(final URL configURL, final LoggerRepository hierarchy) {
    }

    /**
     * Read configuration options from configuration file.
     *
     * @param configFileName The configuration file.
     */
    public static void configure(final String configFileName) {
    }

    /**
     * Read configuration options from url <code>configURL</code>.
     *
     * @param configURL The configuration URL
     */
    public static void configure(final URL configURL) {
    }

    /**
     * Reads configuration options from an InputStream.
     *
     * @param inputStream The input stream
     */
    public static void configure(final InputStream inputStream) {
    }

    /**
     * Read configuration options from <code>properties</code>.
     *
     * See {@link #doConfigure(String, LoggerRepository)} for the expected format.
     *
     * @param properties The properties
     */
    public static void configure(final Properties properties) {
    }

    /**
     * Like {@link #configureAndWatch(String, long)} except that the
     * default delay as defined by FileWatchdog.DEFAULT_DELAY is
     * used.
     *
     * @param configFilename A file in key=value format.
     */
    public static void configureAndWatch(final String configFilename) {
    }

    /**
     * Read the configuration file <code>configFilename</code> if it
     * exists. Moreover, a thread will be created that will periodically
     * check if <code>configFilename</code> has been created or
     * modified. The period is determined by the <code>delay</code>
     * argument. If a change or file creation is detected, then
     * <code>configFilename</code> is read to configure log4j.
     *
     * @param configFilename A file in key=value format.
     * @param delay The delay in milliseconds to wait between each check.
     */
    public static void configureAndWatch(final String configFilename, final long delay) {
    }
}
