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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

class HttpAppenderBuilderTest {

    private HttpAppender.Builder<?> getBuilder() {
        Configuration mockConfig = new DefaultConfiguration();
        return HttpAppender.newBuilder().setConfiguration(mockConfig).setName("TestHttpAppender"); // Name is required
    }

    @Test
    @UsingStatusListener
    void testBuilderWithoutUrl(final ListStatusListener listener) throws Exception {
        HttpAppender appender = HttpAppender.newBuilder()
                .setConfiguration(new DefaultConfiguration())
                .setName("TestAppender")
                .setLayout(JsonLayout.createDefaultLayout()) // Providing a layout here
                .build();

        assertThat(listener.findStatusData(Level.ERROR))
                .anyMatch(statusData ->
                        statusData.getMessage().getFormattedMessage().contains("HttpAppender requires URL to be set."));
    }

    @Test
    @UsingStatusListener
    void testBuilderWithUrlAndWithoutLayout(final ListStatusListener listener) throws Exception {
        HttpAppender appender = HttpAppender.newBuilder()
                .setConfiguration(new DefaultConfiguration())
                .setName("TestAppender")
                .setUrl(new URL("http://localhost:8080/logs"))
                .build();

        assertThat(listener.findStatusData(Level.ERROR)).anyMatch(statusData -> statusData
                .getMessage()
                .getFormattedMessage()
                .contains("HttpAppender requires a layout to be set."));
    }

    @Test
    void testBuilderWithValidConfiguration() throws Exception {
        URL url = new URL("http://example.com");
        Layout<?> layout = JsonLayout.createDefaultLayout();

        HttpAppender.Builder<?> builder = getBuilder().setUrl(url).setLayout(layout);

        HttpAppender appender = builder.build();
        assertNotNull(appender, "HttpAppender should be created with valid configuration.");
    }

    @Test
    void testBuilderWithCustomMethod() throws Exception {
        URL url = new URL("http://example.com");
        Layout<?> layout = JsonLayout.createDefaultLayout();
        String customMethod = "PUT";

        HttpAppender.Builder<?> builder =
                getBuilder().setUrl(url).setLayout(layout).setMethod(customMethod);

        HttpAppender appender = builder.build();
        assertNotNull(appender, "HttpAppender should be created with a custom HTTP method.");
    }

    @Test
    void testBuilderWithHeaders() throws Exception {
        URL url = new URL("http://example.com");
        Layout<?> layout = JsonLayout.createDefaultLayout();
        Property[] headers = new Property[] {
            Property.createProperty("Header1", "Value1"), Property.createProperty("Header2", "Value2")
        };

        HttpAppender.Builder<?> builder =
                getBuilder().setUrl(url).setLayout(layout).setHeaders(headers);

        HttpAppender appender = builder.build();
        assertNotNull(appender, "HttpAppender should be created with headers.");
    }

    @Test
    void testBuilderWithSslConfiguration() throws Exception {
        URL url = new URL("https://example.com");
        Layout<?> layout = JsonLayout.createDefaultLayout();

        // Use real SslConfiguration instead of Mockito mock
        SslConfiguration sslConfig = SslConfiguration.createSSLConfiguration(null, null, null, false);

        HttpAppender.Builder<?> builder =
                getBuilder().setUrl(url).setLayout(layout).setSslConfiguration(sslConfig);

        HttpAppender appender = builder.build();
        assertNotNull(appender, "HttpAppender should be created with SSL configuration.");
    }

    @Test
    void testBuilderWithInvalidUrl() {
        assertThrows(MalformedURLException.class, () -> new URL("invalid-url"));
    }
}
