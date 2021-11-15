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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.net.TcpSocketManager.HostResolver;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests reconnection support of {@link org.apache.logging.log4j.core.appender.SocketAppender}.
 */
class SocketAppenderReconnectTest {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Tests if failures are propagated when reconnection fails.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2829">LOG4J2-2829</a>
     */
    @Test
    void repeating_reconnect_failures_should_be_propagated() throws Exception {
        try (final LineReadingTcpServer server = new LineReadingTcpServer()) {

            // Start the server.
            server.start("Main", 0);
            final int port = server.serverSocket.getLocalPort();

            // Initialize the logger context.
            final LoggerContext loggerContext = initContext(port);
            try {

                // Verify the initial working state.
                verifyLoggingSuccess(server);

                // Stop the server, and verify the logging failure.
                server.close();
                verifyLoggingFailure();

                // Start the server again, and verify the logging success.
                server.start("Main", port);
                verifyLoggingSuccess(server);

            }

            // Shutdown the logger context.
            finally {
                Configurator.shutdown(loggerContext);
            }

        }
    }

    /**
     * Tests if all the {@link InetSocketAddress}es returned by an {@link HostResolver} is used for fallback on reconnect attempts.
     */
    @Test
    void reconnect_should_fallback_when_there_are_multiple_resolved_hosts() throws Exception {
        try (final LineReadingTcpServer primaryServer = new LineReadingTcpServer();
             final LineReadingTcpServer secondaryServer = new LineReadingTcpServer()) {

            // Start servers.
            primaryServer.start("Primary", 0);
            secondaryServer.start("Secondary", 0);

            // Mock the host resolver.
            final FixedHostResolver hostResolver = FixedHostResolver.ofServers(primaryServer, secondaryServer);
            TcpSocketManager.setHostResolver(hostResolver);
            try {

                // Initialize the logger context.
                final LoggerContext loggerContext = initContext(
                        // Passing an invalid port, since the resolution is supposed to be performed by the mocked host resolver anyway.
                        0);
                try {

                    // Verify the initial working state on the primary server.
                    verifyLoggingSuccess(primaryServer);

                    // Stop the primary server, and verify the logging success due to fallback on to the secondary server.
                    primaryServer.close();
                    verifyLoggingSuccess(secondaryServer);

                }

                // Shutdown the logger context.
                finally {
                    Configurator.shutdown(loggerContext);
                }

            } finally {
                // Reset the host resolver.
                TcpSocketManager.setHostResolver(new HostResolver());
            }

        }
    }

    private static LoggerContext initContext(final int port) {

        // Create the configuration builder.
        final ConfigurationBuilder<BuiltConfiguration> configBuilder = ConfigurationBuilderFactory
                .newConfigurationBuilder()
                .setStatusLevel(Level.ERROR)
                .setConfigurationName(SocketAppenderReconnectTest.class.getSimpleName());

        // Create the configuration.
        final String appenderName = "Socket";
        final Configuration config = configBuilder
                .add(configBuilder
                        .newAppender(appenderName, "SOCKET")
                        .addAttribute("host", "localhost")
                        .addAttribute("port", String.valueOf(port))
                        .addAttribute("protocol", Protocol.TCP)
                        .addAttribute("ignoreExceptions", false)
                        .addAttribute("reconnectionDelayMillis", 10)
                        .addAttribute("immediateFlush", true)
                        .add(configBuilder
                                .newLayout("PatternLayout")
                                .addAttribute("pattern", "%m%n")))
                .add(configBuilder.newLogger("org.apache.logging.log4j", Level.DEBUG))
                .add(configBuilder
                        .newRootLogger(Level.ERROR)
                        .add(configBuilder.newAppenderRef(appenderName)))
                .build(false);

        // Initialize the configuration.
        return Configurator.initialize(config);

    }

    private static void verifyLoggingSuccess(final LineReadingTcpServer server) throws Exception {
        final int messageCount = 100;
        // noinspection ConstantConditions
        assertTrue(messageCount > 1, "was expecting messageCount to be bigger than 1 due to LOG4J2-2829, found: " + messageCount);
        final List<String> expectedMessages = IntStream
                .range(0, messageCount)
                .mapToObj(messageIndex -> String.format("m%02d", messageIndex))
                .collect(Collectors.toList());
        final Logger logger = LogManager.getLogger();
        for (int messageIndex = 0; messageIndex < expectedMessages.size(); messageIndex++) {
            final String message = expectedMessages.get(messageIndex);
            // Due to socket initialization, the first write() might need some extra effort.
            if (messageIndex == 0) {
                awaitUntilSucceeds(() -> logger.info(message));
            } else {
                logger.info(message);
            }
        }
        expectedMessages.forEach(logger::info);
        final List<String> actualMessages = server.pollLines(messageCount);
        assertEquals(expectedMessages, actualMessages);
    }

    private static void awaitUntilSucceeds(final Runnable runnable) {
        final long pollIntervalMillis;
        final long timeoutSeconds;
        final boolean osWindows = PropertiesUtil.getProperties().isOsWindows();
        if (osWindows) {
            // Windows-specific non-sense values.
            // These figures are collected by trial-and-error on a friend's laptop which has Windows installed.
            pollIntervalMillis = 1_000L;
            timeoutSeconds = 15;
        } else {
            // Universally sensible values.
            pollIntervalMillis = 1000;
            timeoutSeconds = 3;
        }
        await()
                .pollInterval(pollIntervalMillis, TimeUnit.MILLISECONDS)
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .until(() -> {
                    runnable.run();
                    return true;
                });
    }

    private static void verifyLoggingFailure() {
        final Logger logger = LogManager.getLogger();
        int retryCount = 3;
        // noinspection ConstantConditions
        assertTrue(retryCount > 1, "was expecting retryCount to be bigger than 1 due to LOG4J2-2829, found: " + retryCount);
        for (int i = 0; i < retryCount; i++) {
            try {
                logger.info("should fail #" + i);
                fail("should have failed #" + i);
            } catch (final AppenderLoggingException ignored) {}
        }
    }

    /**
     * A simple TCP server implementation reading the accepted connection's input stream into a blocking queue of lines.
     * <p>
     * The implementation is thread-safe, yet connections are handled sequentially, i.e., no parallelization.
     * The input stream of the connection is decoded in UTF-8.
     * </p>
     */
    private static final class LineReadingTcpServer implements AutoCloseable {

        private volatile boolean running = false;

        private ServerSocket serverSocket = null;

        private Socket clientSocket = null;

        private Thread readerThread = null;

        private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();

        private LineReadingTcpServer() {}

        private synchronized void start(final String name, final int port) throws IOException {
            if (!running) {
                running = true;
                serverSocket = createServerSocket(port);
                readerThread = createReaderThread(name);
            }
        }

        private ServerSocket createServerSocket(final int port) throws IOException {
            final ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(0);   // Zero indicates accept() will block indefinitely.
            return serverSocket;
        }

        private Thread createReaderThread(final String name) {
            final String threadName = "LineReadingTcpSocketServerReader-" + name;
            final Thread thread = new Thread(this::acceptClients, threadName);
            thread.setDaemon(true);     // Avoid blocking JVM exit.
            thread.setUncaughtExceptionHandler((ignored, error) ->
                    LOGGER.error("uncaught reader thread exception", error));
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

            // Accept the client connection.
            final Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (SocketException ignored) {
                return;
            }
            clientSocket.setSoLinger(true, 0);    // Enable immediate forceful close.
            synchronized (this) {
                if (running) {
                    this.clientSocket = clientSocket;
                }
            }

            // Read from the client.
            try (final InputStream clientInputStream = clientSocket.getInputStream();
                 final InputStreamReader clientReader = new InputStreamReader(clientInputStream, StandardCharsets.UTF_8);
                 final BufferedReader clientBufferedReader = new BufferedReader(clientReader)) {
                while (running) {
                    final String line = clientBufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    lines.put(line);
                }
            }

            // Ignore connection failures.
            catch (final SocketException ignored) {}

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

            // Stop the reader, if running.
            Thread stoppedReaderThread = null;
            synchronized (this) {
                if (running) {
                    running = false;
                    // acceptClient() might have closed the client socket due to a connection failure and haven't created a new one yet.
                    // Hence, here we double-check if the client connection is in place.
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        // Interrupting a thread is not sufficient to unblock operations waiting on socket I/O: https://stackoverflow.com/a/4426050/1278899
                        // Hence, here we close the client socket to unblock the read from the client socket.
                        clientSocket.close();
                    }
                    serverSocket.close();
                    stoppedReaderThread = readerThread;
                    clientSocket = null;
                    serverSocket = null;
                    readerThread = null;
                }
            }

            // We wait for the termination of the reader thread outside the synchronized block.
            // Otherwise, there is a chance of deadlock with this join() and the synchronized block inside the acceptClient().
            if (stoppedReaderThread != null) {
                stoppedReaderThread.join();
            }

        }

        private List<String> pollLines(@SuppressWarnings("SameParameterValue") final int count) throws InterruptedException, TimeoutException {
            final List<String> polledLines = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                final String polledLine = pollLine();
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

    /**
     * {@link HostResolver} implementation always resolving to the given list of {@link #addresses}.
     */
    private static final class FixedHostResolver extends HostResolver {

        private final List<InetSocketAddress> addresses;

        private FixedHostResolver(List<InetSocketAddress> addresses) {
            this.addresses = addresses;
        }

        private static FixedHostResolver ofServers(LineReadingTcpServer... servers) {
            List<InetSocketAddress> addresses = Arrays
                    .stream(servers)
                    .map(server -> (InetSocketAddress) server.serverSocket.getLocalSocketAddress())
                    .collect(Collectors.toList());
            return new FixedHostResolver(addresses);
        }

        @Override
        public List<InetSocketAddress> resolveHost(String ignoredHost, int ignoredPort) {
            return addresses;
        }

    }

}
