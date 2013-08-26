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
package org.apache.logging.log4j.core.net;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractServer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.XMLConfiguration;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;

/**
 * Listens for events over a socket connection.
 */
public class UDPSocketServer extends AbstractServer implements Runnable {

    private final Logger logger;

    private static final int MAX_PORT = 65534;

    private volatile boolean isActive = true;

    private final DatagramSocket server;

    // max size so we only have to deal with one packet
    private final int maxBufferSize = 1024 * 65 + 1024;

    /**
     * Constructor.
     *
     * @param port
     *            to listen on.
     * @throws IOException
     *             If an error occurs.
     */
    public UDPSocketServer(final int port) throws IOException {
        this.server = new DatagramSocket(port);
        this.logger = LogManager.getLogger(this.getClass().getName() + '.' + port);
    }

    /**
     * Main startup for the server.
     *
     * @param args
     *            The command line arguments.
     * @throws Exception
     *             if an error occurs.
     */
    public static void main(final String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Incorrect number of arguments");
            printUsage();
            return;
        }
        final int port = Integer.parseInt(args[0]);
        if (port <= 0 || port >= MAX_PORT) {
            System.err.println("Invalid port number");
            printUsage();
            return;
        }
        if (args.length == 2 && args[1].length() > 0) {
            ConfigurationFactory.setConfigurationFactory(new ServerConfigurationFactory(args[1]));
        }
        final UDPSocketServer sserver = new UDPSocketServer(port);
        final Thread server = new Thread(sserver);
        server.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            final String line = reader.readLine();
            if (line == null || line.equalsIgnoreCase("Quit") || line.equalsIgnoreCase("Stop") || line.equalsIgnoreCase("Exit")) {
                sserver.shutdown();
                server.join();
                break;
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: ServerSocket port configFilePath");
    }

    /**
     * Shutdown the server.
     */
    public void shutdown() {
        this.isActive = false;
        Thread.currentThread().interrupt();
    }

    /**
     * Accept incoming events and processes them.
     */
    @Override
    public void run() {
        while (isActive) {
            try {
                final byte[] buf = new byte[maxBufferSize];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                server.receive(packet);
                final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength()));
                final LogEvent event = (LogEvent) ois.readObject();
                if (event != null) {
                    log(event);
                }
            } catch (final OptionalDataException opt) {
                logger.error("OptionalDataException eof=" + opt.eof + " length=" + opt.length, opt);
            } catch (final ClassNotFoundException cnfe) {
                logger.error("Unable to locate LogEvent class", cnfe);
            } catch (final EOFException eofe) {
                logger.info("EOF encountered");
            } catch (final IOException ioe) {
                logger.error("Exception encountered on accept. Ignoring. Stack Trace :", ioe);
            }
        }
    }

    /**
     * Factory that creates a Configuration for the server.
     */
    private static class ServerConfigurationFactory extends XMLConfigurationFactory {

        private final String path;

        public ServerConfigurationFactory(final String path) {
            this.path = path;
        }

        @Override
        public Configuration getConfiguration(final String name, final URI configLocation) {
            if (path != null && path.length() > 0) {
                File file = null;
                ConfigurationSource source = null;
                try {
                    file = new File(path);
                    final FileInputStream is = new FileInputStream(file);
                    source = new ConfigurationSource(is, file);
                } catch (final FileNotFoundException ex) {
                    // Ignore this error
                }
                if (source == null) {
                    try {
                        final URL url = new URL(path);
                        source = new ConfigurationSource(url.openStream(), path);
                    } catch (final MalformedURLException mue) {
                        // Ignore this error
                    } catch (final IOException ioe) {
                        // Ignore this error
                    }
                }

                try {
                    if (source != null) {
                        return new XMLConfiguration(source);
                    }
                } catch (final Exception ex) {
                    // Ignore this error.
                }
                System.err.println("Unable to process configuration at " + path + ", using default.");
            }
            return super.getConfiguration(name, configLocation);
        }
    }
}
