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
package org.apache.log4j.xml;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.spi.LoggerRepository;
import org.w3c.dom.Element;

/**
 *
 */
public class DOMConfigurator {

    public void doConfigure(final String filename, final LoggerRepository repository) {
    }

    public void doConfigure(final URL url, final LoggerRepository repository) {
    }

    public void doConfigure(final InputStream inputStream, final LoggerRepository repository)
        throws FactoryConfigurationError {
    }

    public void doConfigure(final Reader reader, final LoggerRepository repository)
        throws FactoryConfigurationError {
    }

    public void doConfigure(final Element element, final LoggerRepository repository) {
    }

    public static void configure(final Element element) {
    }

    public static void configureAndWatch(final String configFilename) {
    }

    public static void configureAndWatch(final String configFilename, final long delay) {
    }

    public static void configure(final String filename) throws FactoryConfigurationError {
    }

    public static void configure(final URL url) throws FactoryConfigurationError {
    }

    public static String subst(final String value, final Properties props) {
        return value;
    }

    public static void setParameter(final Element elem, final PropertySetter propSetter, final Properties props) {

    }

    public static Object parseElement(final Element element, final Properties props,
                                      @SuppressWarnings("rawtypes") final Class expectedClass)
        throws Exception {
        return null;
    }
}
