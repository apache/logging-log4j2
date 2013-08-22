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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
public class SocketServer extends AbstractServer implements Runnable {

    private final Logger logger;

    private static final int MAX_PORT = 65534;

    private volatile boolean isActive = true;

    private final ServerSocket server;

    private final ConcurrentMap<Long, SocketHandler> handlers = new ConcurrentHashMap<Long, SocketHandler>();

    /**
     * Constructor.
     * @param port to listen on.
     * @throws IOException If an error occurs.
     */
    public SocketServer(final int port) throws IOException {
        this.server = new ServerSocket(port);
        this.logger = LogManager.getLogger(this.getClass().getName() + '.' + port);
    }
     /**
     * Main startup for the server.
     * @param args The command line arguments.
     * @throws Exception if an error occurs.
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
        final SocketServer sserver = new SocketServer(port);
        final Thread server = new Thread(sserver);
        server.start();
        final Charset enc = Charset.defaultCharset();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, enc));
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
                // Accept incoming connections.
                final Socket clientSocket = server.accept();
                clientSocket.setSoLinger(true, 0);

                // accept() will block until a client connects to the server.
                // If execution reaches this point, then it means that a client
                // socket has been accepted.

                final SocketHandler handler = new SocketHandler(clientSocket);
                handlers.put(Long.valueOf(handler.getId()), handler);
                handler.start();
            } catch (final IOException ioe) {
                System.out.println("Exception encountered on accept. Ignoring. Stack Trace :");
                ioe.printStackTrace();
            }
        }
        for (final Map.Entry<Long, SocketHandler> entry : handlers.entrySet()) {
            final SocketHandler handler = entry.getValue();
            handler.shutdown();
            try {
                handler.join();
            } catch (final InterruptedException ie) {
                // Ignore the exception
            }
        }
    }

    /**
     * Thread that processes the events.
     */
    private class SocketHandler extends Thread {
        private final ObjectInputStream ois;

        private boolean shutdown = false;

        public SocketHandler(final Socket socket) throws IOException {

            ois = new ObjectInputStream(socket.getInputStream());
        }

        public void shutdown() {
            this.shutdown = true;
            interrupt();
        }

        @Override
        public void run() {
            boolean closed = false;
            try {
                try {
                    while (!shutdown) {
                        final LogEvent event = (LogEvent) ois.readObject();
                        if (event != null) {
                            log(event);
                        }
                    }
                } catch (final EOFException eof) {
                    closed = true;
                } catch (final OptionalDataException opt) {
                    logger.error("OptionalDataException eof=" + opt.eof + " length=" + opt.length, opt);
                } catch (final ClassNotFoundException cnfe) {
                    logger.error("Unable to locate LogEvent class", cnfe);
                } catch (final IOException ioe) {
                    logger.error("IOException encountered while reading from socket", ioe);
                }
                if (!closed) {
                    try {
                        ois.close();
                    } catch (final Exception ex) {
                        // Ignore the exception;
                    }
                }
            } finally {
                handlers.remove(Long.valueOf(getId()));
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
