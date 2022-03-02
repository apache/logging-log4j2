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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the UrlConnectionFactory
 */
public class UrlConnectionFactoryTest {

    private static final Logger LOGGER = LogManager.getLogger(UrlConnectionFactoryTest.class);
    private static final String BASIC = "Basic ";
    private static final String expectedCreds = "testuser:password";
    private static Server server;
    private static Base64.Decoder decoder = Base64.getDecoder();
    private static int port;
    private static final int NOT_MODIFIED = 304;
    private static final int NOT_AUTHORIZED = 401;
    private static final int OK = 200;
    private static final int BUF_SIZE = 1024;

    @BeforeAll
    public static void startServer() throws Exception {
        try {
            server = new Server(0);
            ServletContextHandler context = new ServletContextHandler();
            ServletHolder defaultServ = new ServletHolder("default", TestServlet.class);
            defaultServ.setInitParameter("resourceBase", System.getProperty("user.dir"));
            defaultServ.setInitParameter("dirAllowed", "true");
            context.addServlet(defaultServ, "/");
            server.setHandler(context);

            // Start Server
            server.start();
            port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @AfterAll
    public static void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void testBadCrdentials() throws Exception {
        System.setProperty("log4j2.Configuration.username", "foo");
        System.setProperty("log4j2.Configuration.password", "bar");
        System.setProperty("log4j2.Configuration.allowedProtocols", "http");
        URI uri = new URI("http://localhost:" + port + "/log4j2-config.xml");
        ConfigurationSource source = ConfigurationSource.fromUri(uri);
        assertNull(source, "A ConfigurationSource should not have been returned");
    }

    @Test
    public void withAuthentication() throws Exception {
        System.setProperty("log4j2.Configuration.username", "testuser");
        System.setProperty("log4j2.Configuration.password", "password");
        System.setProperty("log4j2.Configuration.allowedProtocols", "http");
        URI uri = new URI("http://localhost:" + port + "/log4j2-config.xml");
        ConfigurationSource source = ConfigurationSource.fromUri(uri);
        assertNotNull(source, "No ConfigurationSource returned");
        InputStream is = source.getInputStream();
        assertNotNull(is, "No data returned");
        long lastModified = source.getLastModified();
        int result = verifyNotModified(uri, lastModified);
        assertEquals(NOT_MODIFIED, result,"File was modified");
        File file = new File("target/test-classes/log4j2-config.xml");
        if (!file.setLastModified(System.currentTimeMillis())) {
            fail("Unable to set last modified time");
        }
        result = verifyNotModified(uri, lastModified);
        assertEquals(OK, result,"File was not modified");
    }

    private int verifyNotModified(URI uri, long lastModifiedMillis) throws Exception {
        final HttpURLConnection urlConnection = UrlConnectionFactory.createConnection(uri.toURL(),
                lastModifiedMillis, null);
        urlConnection.connect();

        try {
            return urlConnection.getResponseCode();
        } catch (final IOException ioe) {
            LOGGER.error("Error accessing configuration at {}: {}", uri, ioe.getMessage());
            return 500;
        }
    }

    public static class TestServlet extends DefaultServlet {

        private static final long serialVersionUID = -2885158530511450659L;

        @Override
        protected void doGet(HttpServletRequest request,
                HttpServletResponse response) throws ServletException, IOException {
            Enumeration<String> headers = request.getHeaders(HttpHeader.AUTHORIZATION.toString());
            if (headers == null) {
                response.sendError(401, "No Auth header");
                return;
            }
            while (headers.hasMoreElements()) {
                String authData = headers.nextElement();
                assertTrue(authData.startsWith(BASIC), "Not a Basic auth header");
                String credentials = new String(decoder.decode(authData.substring(BASIC.length())));
                if (!expectedCreds.equals(credentials)) {
                    response.sendError(401, "Invalid credentials");
                    return;
                }
            }
            if (request.getServletPath().equals("/log4j2-config.xml")) {
                File file = new File("target/test-classes/log4j2-config.xml");
                long modifiedSince = request.getDateHeader(HttpHeader.IF_MODIFIED_SINCE.toString());
                long lastModified = (file.lastModified() / 1000) * 1000;
                LOGGER.debug("LastModified: {}, modifiedSince: {}", lastModified, modifiedSince);
                if (modifiedSince > 0 && lastModified <= modifiedSince) {
                    response.setStatus(304);
                    return;
                }
                response.setDateHeader(HttpHeader.LAST_MODIFIED.toString(), lastModified);
                response.setContentLengthLong(file.length());
                Files.copy(file.toPath(), response.getOutputStream());
                response.getOutputStream().flush();
                response.setStatus(200);
            } else {
                response.sendError(400, "Unsupported request");
            }
        }
    }
}
