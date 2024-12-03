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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.util.Objects.requireNonNull;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.logging.log4j.core.config.ConfigurationSourceTest.PATH_IN_JAR;
import static org.apache.logging.log4j.core.net.WireMockUtil.createMapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.sun.management.UnixOperatingSystemMXBean;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.ConfigurationSourceTest;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junitpioneer.jupiter.RetryingTest;

/**
 * Tests the UrlConnectionFactory
 */
@WireMockTest
@SetTestProperty(key = "log4j2.configurationAllowedProtocols", value = "jar,http")
class UrlConnectionFactoryTest {

    private static final Logger LOGGER = LogManager.getLogger(UrlConnectionFactoryTest.class);

    private static final String URL_PATH = "/log4j2-config.xml";
    private static final BasicCredentials CREDENTIALS = new BasicCredentials("testUser", "password");
    private static final byte[] CONFIG_FILE_BODY;
    private static final String CONTENT_TYPE = "application/xml";

    static {
        try (InputStream input = requireNonNull(
                UrlConnectionFactoryTest.class.getClassLoader().getResourceAsStream("log4j2-config.xml"))) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(input, output);
            CONFIG_FILE_BODY = output.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @AfterEach
    void cleanup(WireMockRuntimeInfo info) {
        info.getWireMock().removeMappings();
    }

    @Test
    @SetTestProperty(key = "log4j2.configurationUsername", value = "foo")
    @SetTestProperty(key = "log4j2.configurationPassword", value = "bar")
    void testBadCredentials(WireMockRuntimeInfo info) throws Exception {
        WireMock wireMock = info.getWireMock();
        // RFC 1123 format rounds to full seconds
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        wireMock.importStubMappings(createMapping(URL_PATH, CREDENTIALS, CONFIG_FILE_BODY, CONTENT_TYPE, now));
        final URI uri = new URI(info.getHttpBaseUrl() + URL_PATH);
        final ConfigurationSource source = ConfigurationSource.fromUri(uri);
        assertNull(source, "A ConfigurationSource should not have been returned");
    }

    @Test
    @SetTestProperty(key = "log4j2.configurationUsername", value = "testUser")
    @SetTestProperty(key = "log4j2.configurationPassword", value = "password")
    void withAuthentication(WireMockRuntimeInfo info) throws Exception {
        WireMock wireMock = info.getWireMock();
        // RFC 1123 format rounds to full seconds
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        wireMock.importStubMappings(createMapping(URL_PATH, CREDENTIALS, CONFIG_FILE_BODY, CONTENT_TYPE, now));
        final URI uri = new URI(info.getHttpBaseUrl() + URL_PATH);
        final ConfigurationSource source = ConfigurationSource.fromUri(uri);
        assertNotNull(source, "No ConfigurationSource returned");
        final InputStream is = source.getInputStream();
        assertNotNull(is, "No data returned");
        is.close();
        final long lastModified = source.getLastModified();
        assertThat(lastModified).isEqualTo(now.toInstant().toEpochMilli());
        int result = verifyNotModified(uri, lastModified);
        assertEquals(SC_NOT_MODIFIED, result, "File was modified");

        wireMock.removeMappings();
        now = now.plusMinutes(5);
        wireMock.importStubMappings(createMapping(URL_PATH, CREDENTIALS, CONFIG_FILE_BODY, CONTENT_TYPE, now));
        result = verifyNotModified(uri, lastModified);
        assertEquals(SC_OK, result, "File was not modified");
    }

    private int verifyNotModified(final URI uri, final long lastModifiedMillis) throws Exception {
        AuthorizationProvider provider = ConfigurationFactory.authorizationProvider(PropertiesUtil.getProperties());
        HttpURLConnection urlConnection =
                UrlConnectionFactory.createConnection(uri.toURL(), lastModifiedMillis, null, provider);
        urlConnection.connect();

        try {
            return urlConnection.getResponseCode();
        } catch (final IOException ioe) {
            LOGGER.error("Error accessing configuration at {}: {}", uri, ioe.getMessage());
            return SC_INTERNAL_SERVER_ERROR;
        }
    }

    @RetryingTest(maxAttempts = 5, suspendForMs = 1000)
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Fails frequently on Windows (#2011)")
    @SetTestProperty(key = "log4j2.configurationUsername", value = "testUser")
    @SetTestProperty(key = "log4j2.configurationPassword", value = "password")
    void testNoJarFileLeak(@TempDir Path dir, WireMockRuntimeInfo info) throws Exception {
        Path jarFile = ConfigurationSourceTest.prepareJarConfigURL(dir);
        // Retrieve using 'file:'
        URL jarUrl = new URL("jar:" + jarFile.toUri().toURL() + "!" + PATH_IN_JAR);
        long expected = getOpenFileDescriptorCount();
        UrlConnectionFactory.createConnection(jarUrl).getInputStream().close();
        assertEquals(expected, getOpenFileDescriptorCount());

        // Prepare mock
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try (InputStream inputStream = Files.newInputStream(jarFile)) {
            IOUtils.copy(inputStream, body);
        }
        WireMock wireMock = info.getWireMock();
        wireMock.register(WireMock.get("/jarFile.jar")
                .willReturn(
                        aResponse().withStatus(200).withBodyFile("jarFile.jar").withBody(body.toByteArray())));
        // Retrieve using 'http:'
        jarUrl = new URL("jar:" + info.getHttpBaseUrl() + "/jarFile.jar!" + PATH_IN_JAR);
        // URLConnection leaves JAR files in the temporary directory
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        List<Path> expectedFiles;
        try (Stream<Path> stream = Files.list(tmpDir)) {
            expectedFiles = stream.collect(Collectors.toList());
        }
        UrlConnectionFactory.createConnection(jarUrl).getInputStream().close();
        List<Path> actualFiles;
        try (Stream<Path> stream = Files.list(tmpDir)) {
            actualFiles = stream.collect(Collectors.toList());
        }
        assertThat(actualFiles).containsExactlyElementsOf(expectedFiles);
    }

    private long getOpenFileDescriptorCount() {
        final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        }
        return 0L;
    }
}
