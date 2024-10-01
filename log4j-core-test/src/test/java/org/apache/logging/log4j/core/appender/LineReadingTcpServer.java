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
package org.apache.logging.log4j.core.appender;

import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A simple TCP server implementation reading the accepted connection's input stream into a blocking queue of lines.
 * <p>
 * The implementation is thread-safe, yet connections are handled sequentially, i.e., no parallelization.
 * The input stream of the connection is decoded in UTF-8.
 * </p><p>
 * This class can also be used for secure (i.e., SSL) connections.
 * You need to pass the {@link ServerSocketFactory} obtained from {@link SSLContext#getServerSocketFactory()} to the appropriate constructor.
 * </p>
 */
final class LineReadingTcpServer implements AutoCloseable {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ServerSocketFactory serverSocketFactory;

    private volatile boolean running;

    private ServerSocket serverSocket;

    private Socket clientSocket;

    private Thread readerThread;

    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();

    LineReadingTcpServer() {
        this(ServerSocketFactory.getDefault());
    }

    LineReadingTcpServer(final ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    synchronized void start(final String name, final int port) throws IOException {
        if (!running) {
            running = true;
            serverSocket = createServerSocket(port);
            readerThread = createReaderThread(name);
        }
    }

    private ServerSocket createServerSocket(final int port) throws IOException {
        final ServerSocket serverSocket =
                serverSocketFactory.createServerSocket(port, 1, InetAddress.getLoopbackAddress());
        serverSocket.setReuseAddress(true);
        serverSocket.setSoTimeout(0); // Zero indicates `accept()` will block indefinitely
        await("server socket binding")
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> serverSocket.getLocalPort() != -1 && serverSocket.isBound());
        return serverSocket;
    }

    private Thread createReaderThread(final String name) {
        final String threadName = "LineReadingTcpSocketServerReader-" + name;
        final Thread thread = new Thread(this::acceptClients, threadName);
        thread.setDaemon(true); // Avoid blocking JVM exit
        thread.setUncaughtExceptionHandler((ignored, error) -> LOGGER.error("uncaught reader thread exception", error));
        thread.start();
        return thread;
    }

    private void acceptClients() {
        try {
            while (running) {
                acceptClient();
            }
        } catch (final Exception error) {
            LOGGER.error("failed accepting client connections", error);
        }
    }

    private void acceptClient() throws Exception {

        // Accept the client connection
        final Socket clientSocket;
        try {
            clientSocket = serverSocket.accept();
        } catch (SocketException ignored) {
            return;
        }
        clientSocket.setSoLinger(true, 0); // Enable immediate forceful close
        synchronized (this) {
            if (running) {
                this.clientSocket = clientSocket;
            }
        }

        // Read from the client
        try (final InputStream clientInputStream = clientSocket.getInputStream();
                final InputStreamReader clientReader =
                        new InputStreamReader(clientInputStream, StandardCharsets.UTF_8);
                final BufferedReader clientBufferedReader = new BufferedReader(clientReader)) {
            while (running) {
                final String line = clientBufferedReader.readLine();
                if (line == null) {
                    break;
                }
                lines.put(line);
            }
        } catch (final SSLHandshakeException | EOFException error) {
            // Ignore `EOFException`s
            if (!(error.getCause() instanceof EOFException)) {
                throw error;
            }
        }

        // Ignore connection failures.
        catch (final SocketException ignored) {
        }

        // Clean up the client connection.
        finally {
            try {
                synchronized (this) {
                    if (!clientSocket.isClosed()) {
                        clientSocket.shutdownOutput();
                        clientSocket.close();
                    }
                    this.clientSocket = null;
                }
            } catch (final Exception error) {
                LOGGER.error("failed closing client socket", error);
            }
        }
    }

    @Override
    public void close() throws Exception {

        // Stop the reader, if running
        Thread stoppedReaderThread = null;
        synchronized (this) {
            if (running) {
                running = false;
                // `acceptClient()` might have closed the client socket due to a connection failure and haven't created
                // a new one yet. Hence, here we double-check if the client connection is in place.
                if (clientSocket != null && !clientSocket.isClosed()) {
                    // Interrupting a thread is not sufficient to unblock operations waiting on socket I/O:
                    // https://stackoverflow.com/a/4426050/1278899 Hence, here we close the client socket to unblock the
                    // read from the client socket.
                    clientSocket.close();
                }
                serverSocket.close();
                stoppedReaderThread = readerThread;
                clientSocket = null;
                serverSocket = null;
                readerThread = null;
            }
        }

        // We wait for the termination of the reader thread outside the synchronized block. Otherwise, there is a chance
        // of deadlock with this `join()` and the synchronized block inside the `acceptClient()`.
        if (stoppedReaderThread != null) {
            stoppedReaderThread.join();
        }
    }

    ServerSocket getServerSocket() {
        return serverSocket;
    }

    List<String> pollLines(@SuppressWarnings("SameParameterValue") final int count) throws InterruptedException {
        final List<String> polledLines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final String polledLine;
            try {
                polledLine = pollLine();
            } catch (final TimeoutException timeout) {
                final String message =
                        String.format("timeout while polling for line %d (total needed: %d)", (i + 1), count);
                throw new RuntimeException(message, timeout);
            }
            polledLines.add(polledLine);
        }
        return polledLines;
    }

    private String pollLine() throws InterruptedException, TimeoutException {
        final String line = lines.poll(2, TimeUnit.SECONDS);
        if (line == null) {
            throw new TimeoutException();
        }
        return line;
    }
}
