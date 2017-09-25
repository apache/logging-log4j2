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
package org.apache.logging.log4j.core.appender;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.net.ssl.KeyStoreConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.TestConstants;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

//@Ignore("Fails often on Windows")
public class HttpAppenderTest {

    private static final String LOG_MESSAGE = "Hello, world!";

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
            .setLoggerName(HttpAppenderTest.class.getName())
            .setLoggerFqcn(HttpAppenderTest.class.getName())
            .setLevel(Level.INFO)
            .setMessage(new SimpleMessage(LOG_MESSAGE))
            .build();
    }

    private final ResponseDefinitionBuilder SUCCESS_RESPONSE = aResponse().withStatus(201)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"status\":\"created\"}");

    private final ResponseDefinitionBuilder FAILURE_RESPONSE = aResponse().withStatus(400)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"status\":\"error\"}");

    private final JavaLookup JAVA_LOOKUP = new JavaLookup();

    @Rule
    public LoggerContextRule ctx = new LoggerContextRule("HttpAppenderTest.xml");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort()
        .keystorePath(TestConstants.KEYSTORE_FILE)
        .keystorePassword(String.valueOf(TestConstants.KEYSTORE_PWD()))
        .keystoreType(TestConstants.KEYSTORE_TYPE));

    @Test
    public void testAppend() throws Exception {
        wireMockRule.stubFor(post(urlEqualTo("/test/log4j/"))
            .willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
            .withName("Http")
            .withLayout(JsonLayout.createDefaultLayout())
            .setConfiguration(ctx.getConfiguration())
            .setUrl(new URL("http://localhost:" + wireMockRule.port() + "/test/log4j/"))
            .build();
        appender.append(createLogEvent());

        wireMockRule.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
            .withHeader("Host", containing("localhost"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    @Test
    public void testAppendHttps() throws Exception {
        wireMockRule.stubFor(post(urlEqualTo("/test/log4j/"))
            .willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
            .withName("Http")
            .withLayout(JsonLayout.createDefaultLayout())
            .setConfiguration(ctx.getConfiguration())
            .setUrl(new URL("https://localhost:" + wireMockRule.httpsPort() + "/test/log4j/"))
            .setSslConfiguration(SslConfiguration.createSSLConfiguration(null,
                KeyStoreConfiguration.createKeyStoreConfiguration(TestConstants.KEYSTORE_FILE, TestConstants.KEYSTORE_PWD(), TestConstants.KEYSTORE_TYPE, null),
                TrustStoreConfiguration.createKeyStoreConfiguration(TestConstants.TRUSTSTORE_FILE, TestConstants.TRUSTSTORE_PWD(), TestConstants.TRUSTSTORE_TYPE, null)))
            .setVerifyHostname(false)
            .build();
        appender.append(createLogEvent());

        wireMockRule.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
            .withHeader("Host", containing("localhost"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    @Test
    public void testAppendMethodPut() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/test/log4j/1234"))
            .willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
            .withName("Http")
            .withLayout(JsonLayout.createDefaultLayout())
            .setConfiguration(ctx.getConfiguration())
            .setMethod("PUT")
            .setUrl(new URL("http://localhost:" + wireMockRule.port() + "/test/log4j/1234"))
            .build();
        appender.append(createLogEvent());

        wireMockRule.verify(putRequestedFor(urlEqualTo("/test/log4j/1234"))
            .withHeader("Host", containing("localhost"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    @Test
    public void testAppendCustomHeader() throws Exception {
        wireMockRule.stubFor(post(urlEqualTo("/test/log4j/"))
            .willReturn(SUCCESS_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
            .withName("Http")
            .withLayout(JsonLayout.createDefaultLayout())
            .setConfiguration(ctx.getConfiguration())
            .setUrl(new URL("http://localhost:" + wireMockRule.port() + "/test/log4j/"))
            .setHeaders(new Property[] {
                Property.createProperty("X-Test", "header value"),
                Property.createProperty("X-Runtime", "${java:runtime}")
            })
            .build();
        appender.append(createLogEvent());

        wireMockRule.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
            .withHeader("Host", containing("localhost"))
            .withHeader("X-Test", equalTo("header value"))
            .withHeader("X-Runtime", equalTo(JAVA_LOOKUP.getRuntime()))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));
    }

    volatile StatusData error;

    @Test
    public void testAppendErrorIgnore() throws Exception {
        wireMockRule.stubFor(post(urlEqualTo("/test/log4j/"))
            .willReturn(FAILURE_RESPONSE));

        StatusLogger.getLogger().registerListener(new StatusListener() {
            @Override
            public void log(final StatusData data) {
                error = data;
            }

            @Override
            public Level getStatusLevel() {
                return Level.ERROR;
            }

            @Override
            public void close() throws IOException { }
        });

        error = null;

        final Appender appender = HttpAppender.newBuilder()
            .withName("Http")
            .withLayout(JsonLayout.createDefaultLayout())
            .setConfiguration(ctx.getConfiguration())
            .setUrl(new URL("http://localhost:" + wireMockRule.port() + "/test/log4j/"))
            .build();
        appender.append(createLogEvent());

        wireMockRule.verify(postRequestedFor(urlEqualTo("/test/log4j/"))
            .withHeader("Host", containing("localhost"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(containing("\"message\" : \"" + LOG_MESSAGE + "\"")));

        assertNotNull(error);
        assertEquals(Level.ERROR, error.getLevel());
        assertEquals("Unable to send HTTP in appender [Http]", error.getMessage().toString());
    }

    @Test(expected = AppenderLoggingException.class)
    public void testAppendError() throws Exception {
        wireMockRule.stubFor(post(urlEqualTo("/test/log4j/"))
            .willReturn(FAILURE_RESPONSE));

        final Appender appender = HttpAppender.newBuilder()
            .withName("Http")
            .withLayout(JsonLayout.createDefaultLayout())
            .setConfiguration(ctx.getConfiguration())
            .withIgnoreExceptions(false)
            .setUrl(new URL("http://localhost:" + wireMockRule.port() + "/test/log4j/"))
            .build();
        appender.append(createLogEvent());
    }

    @Test(expected = AppenderLoggingException.class)
    public void testAppendConnectError() throws Exception {
        final Appender appender = HttpAppender.newBuilder()
            .withName("Http")
            .withLayout(JsonLayout.createDefaultLayout())
            .setConfiguration(ctx.getConfiguration())
            .withIgnoreExceptions(false)
            .setUrl(new URL("http://localhost:"+(wireMockRule.port()+1)+"/test/log4j/"))
            .build();
        appender.append(createLogEvent());
    }

}