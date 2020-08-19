package org.apache.logging.log4j.layout.json.template;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;

public class JsonTemplateLayoutNullEventDelimiterTest {

    // Set the configuration.
    static {
        System.setProperty(
                "log4j.configurationFile",
                "nullEventDelimitedJsonTemplateLayoutLogging.xml");
    }

    // Note that this port is hardcoded in the configuration file too!
    private static final int PORT = 50514;

    @Test
    public void test() throws Exception {

        // Set the expected bytes.
        final byte[] expectedBytes = {
                '"', 'f', 'o', 'o', '"', '\0',
                '"', 'b', 'a', 'r', '"', '\0'
        };

        // Start the TCP server.
        try (final TcpServer server = new TcpServer(PORT)) {

            // Produce log events.
            final Logger logger = LogManager.getLogger(JsonTemplateLayoutNullEventDelimiterTest.class);
            logger.log(Level.INFO, "foo");
            logger.log(Level.INFO, "bar");

            // Wait for the log events.
            Awaitility
                    .await()
                    .atMost(Duration.ofSeconds(10))
                    .pollDelay(Duration.ofSeconds(2))
                    .until(() -> server.getTotalReadByteCount() >= expectedBytes.length);

            // Verify the received log events.
            final byte[] actualBytes = server.getReceivedBytes();
            Assertions.assertThat(actualBytes).startsWith(expectedBytes);

        }

    }

    private static final class TcpServer extends Thread implements AutoCloseable {

        private final ServerSocket serverSocket;

        private final ByteArrayOutputStream outputStream;

        private volatile int totalReadByteCount = 0;

        private volatile boolean closed = false;

        private TcpServer(final int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
            this.outputStream = new ByteArrayOutputStream();
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(5_000);
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            try {
                try (final Socket socket = serverSocket.accept()) {
                    final InputStream inputStream = socket.getInputStream();
                    final byte[] buffer = new byte[1024];
                    // noinspection InfiniteLoopStatement
                    while (true) {
                        final int readByteCount = inputStream.read(buffer);
                        if (readByteCount > 0) {
                            synchronized (this) {
                                totalReadByteCount += readByteCount;
                                outputStream.write(buffer, 0, readByteCount);
                            }
                        }
                    }
                }
            } catch (final EOFException ignored) {
                // Socket is closed.
            } catch (final Exception error) {
                if (!closed) {
                    throw new RuntimeException(error);
                }
            }
        }

        public synchronized byte[] getReceivedBytes() {
            return outputStream.toByteArray();
        }

        public synchronized int getTotalReadByteCount() {
            return totalReadByteCount;
        }

        @Override
        public synchronized void close() throws InterruptedException {
            if (closed) {
                throw new IllegalStateException("shutdown has already been invoked");
            }
            closed = true;
            interrupt();
            join(3_000L);
        }

    }

}
