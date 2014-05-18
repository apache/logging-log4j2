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
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.apache.logging.log4j.core.config.ConfigurationFactory;

/**
 * Listens for events over a socket connection.
 * 
 * @param <T>
 *            The kind of input stream read
 */
public class UDPSocketServer<T extends InputStream> extends AbstractSocketServer<T> {

    /**
     * Creates a socket server that reads JSON log events.
     * 
     * @param port
     *            the port to listen
     * @return a new a socket server
     * @throws IOException
     *             if an I/O error occurs when opening the socket.
     */
    public static UDPSocketServer<InputStream> createJsonSocketServer(final int port) throws IOException {
        return new UDPSocketServer<InputStream>(port, new JsonInputStreamLogEventBridge());
    }

    /**
     * Creates a socket server that reads serialized log events.
     * 
     * @param port
     *            the port to listen
     * @return a new a socket server
     * @throws IOException
     *             if an I/O error occurs when opening the socket.
     */
    public static UDPSocketServer<ObjectInputStream> createSerializedSocketServer(final int port) throws IOException {
        return new UDPSocketServer<ObjectInputStream>(port, new ObjectInputStreamLogEventBridge());
    }

    /**
     * Creates a socket server that reads XML log events.
     * 
     * @param port
     *            the port to listen
     * @return a new a socket server
     * @throws IOException
     *             if an I/O error occurs when opening the socket.
     */
    public static UDPSocketServer<InputStream> createXmlSocketServer(final int port) throws IOException {
        return new UDPSocketServer<InputStream>(port, new XmlInputStreamLogEventBridge());
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
        final UDPSocketServer<ObjectInputStream> socketServer = UDPSocketServer.createSerializedSocketServer(port);
        final Thread server = new Thread(socketServer);
        server.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            final String line = reader.readLine();
            if (line == null || line.equalsIgnoreCase("Quit") || line.equalsIgnoreCase("Stop")
                    || line.equalsIgnoreCase("Exit")) {
                socketServer.shutdown();
                server.join();
                break;
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: ServerSocket port configFilePath");
    }

    private final DatagramSocket datagramSocket;

    // max size so we only have to deal with one packet
    private final int maxBufferSize = 1024 * 65 + 1024;

    /**
     * Constructor.
     * 
     * @param port
     *            to listen on.
     * @param logEventInput
     * @throws IOException
     *             If an error occurs.
     */
    public UDPSocketServer(final int port, final LogEventBridge<T> logEventInput) throws IOException {
        super(port, logEventInput);
        this.datagramSocket = new DatagramSocket(port);
    }

    /**
     * Accept incoming events and processes them.
     */
    @Override
    public void run() {
        while (isActive()) {
            if (datagramSocket.isClosed()) {
                // OK we're done.
                return;
            }
            try {
                final byte[] buf = new byte[maxBufferSize];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                datagramSocket.receive(packet);
                final ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
                logEventInput.logEvents(logEventInput.wrapStream(bais), this);
            } catch (final OptionalDataException e) {
                if (datagramSocket.isClosed()) {
                    // OK we're done.
                    return;
                }
                logger.error("OptionalDataException eof=" + e.eof + " length=" + e.length, e);
            } catch (final EOFException e) {
                if (datagramSocket.isClosed()) {
                    // OK we're done.
                    return;
                }
                logger.info("EOF encountered");
            } catch (final IOException e) {
                if (datagramSocket.isClosed()) {
                    // OK we're done.
                    return;
                }
                logger.error("Exception encountered on accept. Ignoring. Stack Trace :", e);
            }
        }
    }

    /**
     * Shutdown the server.
     */
    public void shutdown() {
        this.setActive(false);
        Thread.currentThread().interrupt();
        datagramSocket.close();
    }
}
