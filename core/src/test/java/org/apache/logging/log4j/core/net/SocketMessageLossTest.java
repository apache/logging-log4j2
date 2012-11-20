package org.apache.logging.log4j.core.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SocketMessageLossTest {
    private static final int SOCKET_PORT = 5514;

    private static final String CONFIG = "log4j-socket2.xml";

    @BeforeClass
    public static void before() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
    }

    @Test
    public void testSocket() throws Exception {
        TestSocketServer testServer;
        ExecutorService executor = null;
        Future<InputStream> futureIn;

        try {
            executor = Executors.newSingleThreadExecutor();
            System.err.println("Initializing server");
            testServer = new TestSocketServer();
            futureIn = executor.submit(testServer);

            //System.err.println("Initializing logger");
            Logger logger = LogManager.getLogger(SocketMessageLossTest.class);

            String message = "Log #1";
            logger.error(message);

            BufferedReader reader = new BufferedReader(new InputStreamReader(futureIn.get()));
            assertEquals(message, reader.readLine());

            //System.err.println("Closing server");
            closeQuietly(testServer);
            assertTrue("Server not shutdown", testServer.server.isClosed());

            //System.err.println("Sleeping to ensure no race conditions");
            Thread.sleep(1000);

            message = "Log #2";
            try {
                logger.error(message);
                fail("Expected exception not thrown");
            } catch (AppenderRuntimeException e) {
                // An exception is expected.
            }

            message = "Log #3";
            try {
                logger.error(message);
                fail("Expected exception not thrown");
            } catch (AppenderRuntimeException e) {
                // An exception is expected.
            }
        } finally {
            closeQuietly(executor);
        }
    }


    private static class TestSocketServer implements Callable<InputStream> {
        private ServerSocket server;
        private Socket client;

        public TestSocketServer() throws Exception {
            server = new ServerSocket(SOCKET_PORT);
        }

        public InputStream call() throws Exception {
            client = server.accept();
            return client.getInputStream();
        }

        public void close() {
            closeQuietly(client);
            closeQuietly(server);
        }

        private void closeQuietly(ServerSocket socket) {
            if (null != socket) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }

        private void closeQuietly(Socket socket) {
            if (null != socket) {
                try {
                    socket.setSoLinger(true, 0);
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static void closeQuietly(ExecutorService executor) {
        if (null != executor) {
            executor.shutdownNow();
        }
    }

    private static void closeQuietly(TestSocketServer testServer) {
        if (null != testServer) {
            testServer.close();
        }
    }
}
