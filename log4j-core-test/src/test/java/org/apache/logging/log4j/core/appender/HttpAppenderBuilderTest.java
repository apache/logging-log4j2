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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.net.ssl.KeyStoreConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslKeyStoreConstants;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

@StatusLoggerLevel(Level.ERROR) // Ensure the logger captures ERROR level messages
class HttpAppenderBuilderTest {

    @Test
    @UsingStatusListener
    void testMissingLayout(final ListStatusListener listener) throws Exception {
        // Build the appender without a layout
        HttpAppender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setUrl(new URL("https://localhost"))
                .build();

        // Assert that the appender is null
        assertNull(appender, "Appender should be null when layout is missing");

        // Verify that an ERROR log message was recorded
        assertTrue(
                listener.findStatusData(Level.ERROR)
                        .stream()
                        .anyMatch(status -> status.getMessage().getFormattedMessage()
                                .contains("No layout configured for HttpAppender 'Http'")),
                "Expected error message was not logged"
        );
    }

    @Test
    @UsingStatusListener
    void testMissingUrl(final ListStatusListener listener) {
        // Build the appender without a URL
        HttpAppender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setLayout(new JsonLayout.Builder().build())
                .build();

        // Assert that the appender is null
        assertNull(appender, "Appender should be null when URL is missing");

        // Verify that an ERROR log message was recorded
        assertTrue(
                listener.findStatusData(Level.ERROR)
                        .stream()
                        .anyMatch(status -> status.getMessage().getFormattedMessage()
                                .contains("No URL configured for HttpAppender 'Http'")),
                "Expected error message was not logged"
        );
    }

    @Test
    @UsingStatusListener
    void testMissingName(final ListStatusListener listener) throws Exception {
        // Build the appender without a name
        HttpAppender appender = HttpAppender.newBuilder()
                .setUrl(new URL("https://localhost"))
                .setLayout(new JsonLayout.Builder().build())
                .build();

        // Assert that the appender is null
        assertNull(appender, "Appender should be null when name is missing");

        // Verify that an ERROR log message was recorded
        assertTrue(
                listener.findStatusData(Level.ERROR)
                        .stream()
                        .anyMatch(status -> status.getMessage().getFormattedMessage()
                                .contains("No name configured for HttpAppender")),
                "Expected error message was not logged"
        );
    }

    @Test
    void testValidAppenderCreation() throws Exception {
        // Build the appender with all required properties
        HttpAppender appender = HttpAppender.newBuilder()
                .setName("Http")
                .setUrl(new URL("https://localhost"))
                .setLayout(new JsonLayout.Builder().build())
                .build();

        // Assert that the appender is not null
        assertNotNull(appender, "Appender should be created successfully with valid properties");
    }
}
