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
package org.apache.logging.log4j.core.filter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for simple App.
 */
public class HttpThreadContextMapFilterTest implements MutableThreadContextMapFilter.FilterConfigUpdateListener {

    private static final String BASIC = "Basic ";
    private static final String expectedCreds = "log4j:log4j";
    private static Server server;
    private static final Base64.Decoder decoder = Base64.getDecoder();
    private static int port;
    static final String CONFIG = "log4j2-mutableFilter.xml";
    static LoggerContext loggerContext = null;
    static final File targetFile = new File("target/test-classes/testConfig.json");
    static final Path target = targetFile.toPath();
    CountDownLatch updated = new CountDownLatch(1);


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
            try {
                Files.deleteIfExists(target);
            } catch (IOException ioe) {
                // Ignore this.
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @AfterAll
    public static void stopServer() throws Exception {
        server.stop();
    }
    @AfterEach
    public void after() {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ioe) {
            // Ignore this.
        }
        loggerContext.stop();
        loggerContext = null;
    }

    @Test
    public void filterTest() throws Exception {
        String contextName = getClass().getSimpleName();
        String configLocation = "http://localhost:" + port + "/testConfig.json";
        System.setProperty("log4j2.HttpThreadContextMapFilterTest.Configuration.location", configLocation);
        System.setProperty("log4j2.HttpThreadContextMapFilterTest.Configuration.allowedProtocols", "http");
        System.setProperty("log4j2.HttpThreadContextMapFilterTest.Configuration.TransportSecurity.basicUsername", "log4j");
        System.setProperty("log4j2.HttpThreadContextMapFilterTest.Configuration.TransportSecurity.basicPassword", "log4j");
        ThreadContext.put("loginId", "rgoers");
        Path source = new File("target/test-classes/emptyConfig.json").toPath();
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        long fileTime = targetFile.lastModified() - 2000;
        assertTrue(targetFile.setLastModified(fileTime));
        loggerContext = Configurator.initialize(contextName, CONFIG);
        assertNotNull(loggerContext);
        Appender app = loggerContext.getConfiguration().getAppender("List");
        assertNotNull(app);
        assertTrue(app instanceof ListAppender);
        MutableThreadContextMapFilter filter = (MutableThreadContextMapFilter) loggerContext.getConfiguration().getFilter();
        assertNotNull(filter);
        filter.registerListener(this);
        Logger logger = loggerContext.getLogger("Test");
        logger.debug("This is a test");
        assertEquals(0, ((ListAppender) app).getEvents().size());
        source = new File("target/test-classes/filterConfig.json").toPath();
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        assertNotEquals(fileTime, targetFile.lastModified());
        if (!updated.await(5, TimeUnit.SECONDS)) {
            fail("File update was not detected");
        }
        updated = new CountDownLatch(1);
        logger.debug("This is a test");
        assertEquals(1, ((ListAppender) app).getEvents().size());
        assertTrue(Files.deleteIfExists(target));
        if (!updated.await(5, TimeUnit.SECONDS)) {
            fail("File update for delete was not detected");
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
            if (request.getServletPath().equals("/testConfig.json")) {
                File file = new File("target/test-classes/testConfig.json");
                if (!file.exists()) {
                    response.sendError(404, "File not found");
                    return;
                }
                long modifiedSince = request.getDateHeader(HttpHeader.IF_MODIFIED_SINCE.toString());
                long lastModified = (file.lastModified() / 1000) * 1000;
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

    @Override
    public void onEvent() {
        updated.countDown();
    }
}
