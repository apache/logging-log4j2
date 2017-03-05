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
package org.apache.logging.log4j.core.net.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEventListener;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.util.BasicCommandLineArguments;
import org.apache.logging.log4j.core.util.InetAddressConverter;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.util.Strings;

/**
 * Abstract socket server for TCP and UDP implementations.
 *
 * @param <T>
 *            The kind of input stream read
 *
 *            TODO Make a LifeCycle
 */
public abstract class AbstractSocketServer<T extends InputStream> extends LogEventListener implements Runnable {

    protected static class CommandLineArguments extends BasicCommandLineArguments {

        @Parameter(names = { "--config", "-c" }, description = "Log4j configuration file location (path or URL).")
        private String configLocation;

        @Parameter(names = { "--interactive",
                "-i" }, description = "Accepts commands on standard input (\"exit\" is the only command).")
        private boolean interactive;

        @Parameter(names = { "--port",
                "-p" }, validateWith = PositiveInteger.class, description = "Server socket port.")
        private int port;

        @Parameter(names = { "--localbindaddress",
                "-a" }, converter = InetAddressConverter.class, description = "Server socket local bind address.")
        private InetAddress localBindAddress;

        String getConfigLocation() {
            return configLocation;
        }

        int getPort() {
            return port;
        }

        protected boolean isInteractive() {
            return interactive;
        }

        void setConfigLocation(final String configLocation) {
            this.configLocation = configLocation;
        }

        void setInteractive(final boolean interactive) {
            this.interactive = interactive;
        }

        void setPort(final int port) {
            this.port = port;
        }

        InetAddress getLocalBindAddress() {
            return localBindAddress;
        }

        void setLocalBindAddress(final InetAddress localBindAddress) {
            this.localBindAddress = localBindAddress;
        }
    }

    /**
     * Factory that creates a Configuration for the server.
     */
    protected static class ServerConfigurationFactory extends XmlConfigurationFactory {

        private final String path;

        public ServerConfigurationFactory(final String path) {
            this.path = path;
        }

        @Override
        public Configuration getConfiguration(final LoggerContext loggerContext, final String name,
                final URI configLocation) {
            if (Strings.isNotEmpty(path)) {
                File file = null;
                ConfigurationSource source = null;
                try {
                    file = new File(path);
                    final FileInputStream is = new FileInputStream(file);
                    source = new ConfigurationSource(is, file);
                } catch (final FileNotFoundException ignored) {
                    // Ignore this error
                }
                if (source == null) {
                    try {
                        final URL url = new URL(path);
                        source = new ConfigurationSource(url.openStream(), url);
                    } catch (final IOException ignored) {
                        // Ignore this error
                    }
                }

                try {
                    if (source != null) {
                        return new XmlConfiguration(loggerContext, source);
                    }
                } catch (final Exception ignored) {
                    // Ignore this error.
                }
                System.err.println("Unable to process configuration at " + path + ", using default.");
            }
            return super.getConfiguration(loggerContext, name, configLocation);
        }
    }

    protected static final int MAX_PORT = 65534;

    private volatile boolean active = true;

    protected final LogEventBridge<T> logEventInput;

    protected final Logger logger;

    /**
     * Creates a new socket server.
     *
     * @param port
     *            listen to this port
     * @param logEventInput
     *            Use this input to read log events.
     */
    public AbstractSocketServer(final int port, final LogEventBridge<T> logEventInput) {
        this.logger = LogManager.getLogger(this.getClass().getName() + '.' + port);
        this.logEventInput = Objects.requireNonNull(logEventInput, "LogEventInput");
    }

    protected boolean isActive() {
        return this.active;
    }

    protected void setActive(final boolean isActive) {
        this.active = isActive;
    }

    /**
     * Start this server in a new thread.
     *
     * @return the new thread that running this server.
     */
    public Thread startNewThread() {
        final Thread thread = new Log4jThread(this);
        thread.start();
        return thread;
    }

    public abstract void shutdown() throws Exception;

    public void awaitTermination(final Thread serverThread) throws Exception {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            final String line = reader.readLine();
            if (line == null
                || line.equalsIgnoreCase("quit")
                || line.equalsIgnoreCase("stop")
                || line.equalsIgnoreCase("exit")) {
                this.shutdown();
                serverThread.join();
                break;
            }
        }
    }

}
