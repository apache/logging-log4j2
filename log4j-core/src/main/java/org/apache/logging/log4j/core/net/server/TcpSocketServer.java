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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.core.config.ConfigurationFactory;

/**
 * Listens for events over a socket connection.
 * 
 * @param <T>
 *        The kind of input stream read
 */
public class TcpSocketServer<T extends InputStream> extends AbstractSocketServer<T> {

    /**
     * Thread that processes the events.
     */
    private class SocketHandler extends Thread {

        private final T inputStream;

        private volatile boolean shutdown = false;

        public SocketHandler(final Socket socket) throws IOException {
            this.inputStream = logEventInput.wrapStream(socket.getInputStream());
        }

        @Override
        public void run() {
            logger.entry();
            boolean closed = false;
            try {
                try {
                    while (!shutdown) {
                        logEventInput.logEvents(inputStream, TcpSocketServer.this);
                    }
                } catch (final EOFException e) {
                    closed = true;
                } catch (final OptionalDataException e) {
                    logger.error("OptionalDataException eof=" + e.eof + " length=" + e.length, e);
                } catch (final IOException e) {
                    logger.error("IOException encountered while reading from socket", e);
                }
                if (!closed) {
                    try {
                        inputStream.close();
                    } catch (final Exception ex) {
                        // Ignore the exception;
                    }
                }
            } finally {
                handlers.remove(Long.valueOf(getId()));
            }
            logger.exit();
        }

        public void shutdown() {
            this.shutdown = true;
            interrupt();
        }
    }

    private final ConcurrentMap<Long, SocketHandler> handlers = new ConcurrentHashMap<>();

    private final ServerSocket serverSocket;

    /**
     * Constructor.
     * 
     * @param port
     *        to listen.
     * @param logEventInput
     *        the log even input
     * @throws IOException
     *         if an I/O error occurs when opening the socket.
     */
    public TcpSocketServer(final int port, final LogEventBridge<T> logEventInput) throws IOException {
        this(port, logEventInput, new ServerSocket(port));
    }

    /**
     * Constructor.
     * 
     * @param port
     *        to listen.
     * @param logEventInput
     *        the log even input
     * @param serverSocket
     *        the socket server
     * @throws IOException
     *         if an I/O error occurs when opening the socket.
     */
    public TcpSocketServer(final int port, final LogEventBridge<T> logEventInput, final ServerSocket serverSocket)
            throws IOException {
        super(port, logEventInput);
        this.serverSocket = serverSocket;
    }

    /**
     * Creates a socket server that reads JSON log events.
     * 
     * @param port
     *        the port to listen
     * @return a new a socket server
     * @throws IOException
     *         if an I/O error occurs when opening the socket.
     */
    public static TcpSocketServer<InputStream> createJsonSocketServer(final int port) throws IOException {
        LOGGER.entry("createJsonSocketServer", port);
        final TcpSocketServer<InputStream> socketServer = new TcpSocketServer<>(port, new JsonInputStreamLogEventBridge());
        return LOGGER.exit(socketServer);
    }

    /**
     * Creates a socket server that reads serialized log events.
     * 
     * @param port
     *        the port to listen
     * @return a new a socket server
     * @throws IOException
     *         if an I/O error occurs when opening the socket.
     */
    public static TcpSocketServer<ObjectInputStream> createSerializedSocketServer(final int port) throws IOException {
        LOGGER.entry(port);
        final TcpSocketServer<ObjectInputStream> socketServer = new TcpSocketServer<>(port, new ObjectInputStreamLogEventBridge());
        return LOGGER.exit(socketServer);
    }

    /**
     * Creates a socket server that reads XML log events.
     * 
     * @param port
     *        the port to listen
     * @return a new a socket server
     * @throws IOException
     *         if an I/O error occurs when opening the socket.
     */
    public static TcpSocketServer<InputStream> createXmlSocketServer(final int port) throws IOException {
        LOGGER.entry(port);
        final TcpSocketServer<InputStream> socketServer = new TcpSocketServer<>(port, new XmlInputStreamLogEventBridge());
        return LOGGER.exit(socketServer);
    }

    /**
     * Main startup for the server.
     * 
     * @param args
     *        The command line arguments.
     * @throws Exception
     *         if an error occurs.
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
        final TcpSocketServer<ObjectInputStream> socketServer = TcpSocketServer.createSerializedSocketServer(port);
        final Thread serverThread = new Thread(socketServer);
        serverThread.start();
        final Charset enc = Charset.defaultCharset();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, enc));
        while (true) {
            final String line = reader.readLine();
            if (line == null || line.equalsIgnoreCase("Quit") || line.equalsIgnoreCase("Stop")
                    || line.equalsIgnoreCase("Exit")) {
                socketServer.shutdown();
                serverThread.join();
                break;
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: ServerSocket port configFilePath");
    }

    /**
     * Accept incoming events and processes them.
     */
    @Override
    public void run() {
        logger.entry();
        while (isActive()) {
            if (serverSocket.isClosed()) {
                return;
            }
            try {
                // Accept incoming connections.
                logger.debug("Socket accept()...");
                final Socket clientSocket = serverSocket.accept();
                logger.debug("Socket accepted: {}", clientSocket);
                clientSocket.setSoLinger(true, 0);

                // accept() will block until a client connects to the server.
                // If execution reaches this point, then it means that a client
                // socket has been accepted.

                final SocketHandler handler = new SocketHandler(clientSocket);
                handlers.put(Long.valueOf(handler.getId()), handler);
                handler.start();
            } catch (final IOException e) {
                if (serverSocket.isClosed()) {
                    // OK we're done.
                    logger.exit();
                    return;
                }
                logger.error("Exception encountered on accept. Ignoring. Stack Trace :", e);
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
        logger.exit();
    }

    /**
     * Shutdown the server.
     * 
     * @throws IOException if the server socket could not be closed
     */
    public void shutdown() throws IOException {
        logger.entry();
        setActive(false);
        Thread.currentThread().interrupt();
        serverSocket.close();
        logger.exit();
    }
}
