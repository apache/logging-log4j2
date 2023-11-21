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
package org.apache.logging.log4j.core.filter;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ThreadContextMapFilter} using a WireMock stub.
 */
public class HttpThreadContextMapFilterTest {

    @Test
    @LoggerContextSource
    public void wireMock_logs_should_be_filtered_on_MDC(
            final LoggerContext loggerContext, @Named("List") final ListAppender appender) throws Exception {

        // Create the logger
        final ExtendedLogger logger = loggerContext.getLogger(HttpThreadContextMapFilterTest.class);

        // Create a response transformer; the only way to dynamically construct WireMock responses.
        // We need a dynamic response generation, since there we will issue MDC changes and log statements.
        final ResponseTransformer wireMockResponseTransformer = new ResponseTransformer() {

            private final AtomicInteger invocationCounter = new AtomicInteger();

            @Override
            public Response transform(
                    final Request request,
                    final Response response,
                    final FileSource files,
                    final Parameters parameters) {
                final int invocationCount = invocationCounter.getAndIncrement();
                ThreadContext.put("invocationCount", "" + invocationCount);
                logger.info("transforming request #{}", invocationCount);
                return response;
            }

            @Override
            public String getName() {
                return "mdc-writer";
            }
        };

        // Create the WireMock server extended using the response transformer.
        final WireMockServer wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig().dynamicPort().extensions(wireMockResponseTransformer));
        wireMockServer.stubFor(get("/").willReturn(ok().withTransformers(wireMockResponseTransformer.getName())));

        wireMockServer.start();
        try {

            // Perform some HTTP requests.
            // `HttpThreadContextMapFilterTest.xml` only allows when `invocationCount={0,2}`.
            // Hence, there preferably needs to be more than 2 requests.
            final String wireMockStubUrl = wireMockServer.url("/");
            httpGet(wireMockStubUrl);
            httpGet(wireMockStubUrl);
            httpGet(wireMockStubUrl);
            httpGet(wireMockStubUrl);
            httpGet(wireMockStubUrl);

            // Verify that `invocationCount={0,2}` filter in `HttpThreadContextMapFilterTest.xml` works
            assertThat(appender.getMessages()).containsOnly("transforming request #0", "transforming request #2");

        } finally {
            wireMockServer.stop();
        }
    }

    private static void httpGet(String url) throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        try {
            assertThat(connection.getResponseCode()).isEqualTo(HttpURLConnection.HTTP_OK);
        } finally {
            connection.disconnect();
        }
    }
}
