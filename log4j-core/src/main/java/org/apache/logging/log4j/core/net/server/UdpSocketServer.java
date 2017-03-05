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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.BasicCommandLineArguments;

/**
 * Listens for Log4j events on a datagram socket and passes them on to Log4j. 
 * 
 * @param <T>
 *            The kind of input stream read
 * @see #main(String[])
 */
public class UdpSocketServer<T extends InputStream> extends AbstractSocketServer<T> {

    /**
     * Creates a socket server that reads JSON log events.
     * 
     * @param port
     *            the port to listen
     * @return a new a socket server
     * @throws IOException
     *             if an I/O error occurs when opening the socket.
     */
    public static UdpSocketServer<InputStream> createJsonSocketServer(final int port) throws IOException {
        return new UdpSocketServer<>(port, new JsonInputStreamLogEventBridge());
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
    public static UdpSocketServer<ObjectInputStream> createSerializedSocketServer(final int port) throws IOException {
        return new UdpSocketServer<>(port, new ObjectInputStreamLogEventBridge());
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
    public static UdpSocketServer<InputStream> createXmlSocketServer(final int port) throws IOException {
        return new UdpSocketServer<>(port, new XmlInputStreamLogEventBridge());
    }

    /**
     * Main startup for the server. Run with "--help" for to print command line help on the console.
     * 
     * @param args
     *            The command line arguments.
     * @throws Exception
     *             if an error occurs.
     */
    public static void main(final String[] args) throws Exception {
        final CommandLineArguments cla = BasicCommandLineArguments.parseCommandLine(args, UdpSocketServer.class, new CommandLineArguments());
        if (cla.isHelp()) {
            return;
        }
        if (cla.getConfigLocation() != null) {
            ConfigurationFactory.setConfigurationFactory(new ServerConfigurationFactory(cla.getConfigLocation()));
        }
        final UdpSocketServer<ObjectInputStream> socketServer = UdpSocketServer
                .createSerializedSocketServer(cla.getPort());
        final Thread serverThread = socketServer.startNewThread();
        if (cla.isInteractive()) {
            socketServer.awaitTermination(serverThread);
        }
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
    public UdpSocketServer(final int port, final LogEventBridge<T> logEventInput) throws IOException {
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
    @Override
    public void shutdown() {
        this.setActive(false);
        Thread.currentThread().interrupt();
        datagramSocket.close();
    }
}
