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
package org.apache.logging.log4j.core.net;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.management.UnixOperatingSystemMXBean;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.ConfigurationSourceTest;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the UrlConnectionFactory
 */
public class UrlConnectionFactoryTest {

    private static final Logger LOGGER = LogManager.getLogger(UrlConnectionFactoryTest.class);
    private static final String BASIC = "Basic ";
    private static final String expectedCreds = "testuser:password";
    private static Server server;
    private static final Base64.Decoder decoder = Base64.getDecoder();
    private static int port;
    private static final int BUF_SIZE = 1024;

    @BeforeAll
    public static void startServer() throws Exception {
        try {
            server = new Server(0);
            final ServletContextHandler context = new ServletContextHandler();
            final ServletHolder defaultServ = new ServletHolder("default", TestServlet.class);
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
        final URI uri = new URI("http://localhost:" + port + "/log4j2-config.xml");
        final ConfigurationSource source = ConfigurationSource.fromUri(uri);
        assertNull(source, "A ConfigurationSource should not have been returned");
    }

    @Test
    public void withAuthentication() throws Exception {
        System.setProperty("log4j2.Configuration.username", "testuser");
        System.setProperty("log4j2.Configuration.password", "password");
        System.setProperty("log4j2.Configuration.allowedProtocols", "http");
        final URI uri = new URI("http://localhost:" + port + "/log4j2-config.xml");
        final ConfigurationSource source = ConfigurationSource.fromUri(uri);
        assertNotNull(source, "No ConfigurationSource returned");
        final InputStream is = source.getInputStream();
        assertNotNull(is, "No data returned");
        is.close();
        final long lastModified = source.getLastModified();
        int result = verifyNotModified(uri, lastModified);
        assertEquals(SC_NOT_MODIFIED, result, "File was modified");
        final File file = new File("target/classes/log4j2-config.xml");
        if (!file.setLastModified(System.currentTimeMillis())) {
            fail("Unable to set last modified time");
        }
        result = verifyNotModified(uri, lastModified);
        assertEquals(SC_OK, result, "File was not modified");
    }

    private int verifyNotModified(final URI uri, final long lastModifiedMillis) throws Exception {
        final HttpURLConnection urlConnection =
                UrlConnectionFactory.createConnection(uri.toURL(), lastModifiedMillis, null, null);
        urlConnection.connect();

        try {
            return urlConnection.getResponseCode();
        } catch (final IOException ioe) {
            LOGGER.error("Error accessing configuration at {}: {}", uri, ioe.getMessage());
            return SC_INTERNAL_SERVER_ERROR;
        }
    }

    @Test
    public void testNoJarFileLeak() throws Exception {
        ConfigurationSourceTest.prepareJarConfigURL();
        final URL url = new File("target/test-classes/jarfile.jar").toURI().toURL();
        // Retrieve using 'file:'
        URL jarUrl = new URL("jar:" + url.toString() + "!/config/console.xml");
        long expected = getOpenFileDescriptorCount();
        UrlConnectionFactory.createConnection(jarUrl).getInputStream().close();
        assertEquals(expected, getOpenFileDescriptorCount());
        // Retrieve using 'http:'
        jarUrl = new URL("jar:http://localhost:" + port + "/jarfile.jar!/config/console.xml");
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        expected = tmpDir.list().length;
        UrlConnectionFactory.createConnection(jarUrl).getInputStream().close();
        assertEquals(expected, tmpDir.list().length, "File descriptor leak");
    }

    private long getOpenFileDescriptorCount() {
        final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        }
        return 0L;
    }

    public static class TestServlet extends DefaultServlet {

        private static final long serialVersionUID = -2885158530511450659L;

        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
                throws ServletException, IOException {
            final Enumeration<String> headers = request.getHeaders(HttpHeader.AUTHORIZATION.toString());
            if (headers == null) {
                response.sendError(SC_UNAUTHORIZED, "No Auth header");
                return;
            }
            while (headers.hasMoreElements()) {
                final String authData = headers.nextElement();
                assertTrue(authData.startsWith(BASIC), "Not a Basic auth header");
                final String credentials = new String(decoder.decode(authData.substring(BASIC.length())));
                if (!expectedCreds.equals(credentials)) {
                    response.sendError(SC_UNAUTHORIZED, "Invalid credentials");
                    return;
                }
            }
            final String servletPath = request.getServletPath();
            if (servletPath != null) {
                File file = new File("target/classes" + servletPath);
                if (!file.exists()) {
                    file = new File("target/test-classes" + servletPath);
                }
                if (!file.exists()) {
                    response.sendError(SC_NOT_FOUND);
                    return;
                }
                final long modifiedSince = request.getDateHeader(HttpHeader.IF_MODIFIED_SINCE.toString());
                final long lastModified = (file.lastModified() / 1000) * 1000;
                LOGGER.debug("LastModified: {}, modifiedSince: {}", lastModified, modifiedSince);
                if (modifiedSince > 0 && lastModified <= modifiedSince) {
                    response.setStatus(SC_NOT_MODIFIED);
                    return;
                }
                response.setDateHeader(HttpHeader.LAST_MODIFIED.toString(), lastModified);
                response.setContentLengthLong(file.length());
                Files.copy(file.toPath(), response.getOutputStream());
                response.getOutputStream().flush();
                response.setStatus(SC_OK);
            } else {
                response.sendError(SC_BAD_REQUEST, "Unsupported request");
            }
        }
    }
}
