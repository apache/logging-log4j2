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
package org.apache.log4j.xml;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.xml.parsers.FactoryConfigurationError;
import org.apache.log4j.LogManager;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.util.IOUtils;
import org.w3c.dom.Element;

/**
 * Use this class to initialize the log4j environment using a DOM tree.
 *
 * <p>
 * The DTD is specified in <a href="doc-files/log4j.dtd"><b>log4j.dtd</b></a>.
 *
 * <p>
 * Sometimes it is useful to see how log4j is reading configuration files. You can enable log4j internal logging by
 * defining the <b>log4j.debug</b> variable on the java command line. Alternatively, set the <code>debug</code>
 * attribute in the <code>log4j:configuration</code> element. As in
 *
 * <pre>
 * &lt;log4j:configuration <b>debug="true"</b> xmlns:log4j="http://jakarta.apache.org/log4j/">
 * ...
 * &lt;/log4j:configuration>
 * </pre>
 *
 * <p>
 * There are sample XML files included in the package.
 *
 * @since 0.8.3
 */
public class DOMConfigurator {

    public static void configure(final Element element) {}

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "The filename comes from a system property.")
    public static void configure(final String fileName) throws FactoryConfigurationError {
        final Path path = Paths.get(fileName);
        try (final InputStream inputStream = Files.newInputStream(path)) {
            final ConfigurationSource source = new ConfigurationSource(inputStream, path);
            final LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            Configuration configuration;
            configuration = new XmlConfigurationFactory().getConfiguration(context, source);
            LogManager.getRootLogger().removeAllAppenders();
            Configurator.reconfigure(configuration);
        } catch (final IOException e) {
            throw new FactoryConfigurationError(e);
        }
    }

    public static void configure(final URL url) throws FactoryConfigurationError {
        new DOMConfigurator().doConfigure(url, LogManager.getLoggerRepository());
    }

    public static void configureAndWatch(final String fileName) {
        // TODO Watch
        configure(fileName);
    }

    public static void configureAndWatch(final String fileName, final long delay) {
        final XMLWatchdog xdog = new XMLWatchdog(fileName);
        xdog.setDelay(delay);
        xdog.start();
    }

    public static Object parseElement(
            final Element element, final Properties props, @SuppressWarnings("rawtypes") final Class expectedClass) {
        return null;
    }

    public static void setParameter(final Element elem, final PropertySetter propSetter, final Properties props) {}

    public static String subst(final String value, final Properties props) {
        return OptionConverter.substVars(value, props);
    }

    private void doConfigure(final ConfigurationSource source) {
        final LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Configuration configuration;
        configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        Configurator.reconfigure(configuration);
    }

    public void doConfigure(final Element element, final LoggerRepository repository) {}

    public void doConfigure(final InputStream inputStream, final LoggerRepository repository)
            throws FactoryConfigurationError {
        try {
            doConfigure(new ConfigurationSource(inputStream));
        } catch (final IOException e) {
            throw new FactoryConfigurationError(e);
        }
    }

    public void doConfigure(final Reader reader, final LoggerRepository repository) throws FactoryConfigurationError {
        try {
            final StringWriter sw = new StringWriter();
            IOUtils.copy(reader, sw);
            doConfigure(new ConfigurationSource(
                    new ByteArrayInputStream(sw.toString().getBytes(StandardCharsets.UTF_8))));
        } catch (final IOException e) {
            throw new FactoryConfigurationError(e);
        }
    }

    public void doConfigure(final String fileName, final LoggerRepository repository) {
        configure(fileName);
    }

    public void doConfigure(final URL url, final LoggerRepository repository) {
        try {
            final URLConnection connection = UrlConnectionFactory.createConnection(url);
            try (final InputStream inputStream = connection.getInputStream()) {
                doConfigure(new ConfigurationSource(inputStream, url));
            }
        } catch (final IOException e) {
            throw new FactoryConfigurationError(e);
        }
    }
}
