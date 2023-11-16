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
package org.apache.logging.log4j.layout.template.json;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.logging.log4j2.EcsLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.apache.logging.log4j.layout.template.json.util.ThreadLocalRecyclerFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
class LogstashIT {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final DefaultConfiguration CONFIGURATION = new DefaultConfiguration();

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String HOST_NAME = NetUtils.getLocalHostname();

    private static final String SERVICE_NAME = "LogstashIT";

    private static final String EVENT_DATASET = SERVICE_NAME + ".log";

    private static final GelfLayout GELF_LAYOUT = GelfLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setCompressionType(GelfLayout.CompressionType.OFF)
            .setIncludeNullDelimiter(true)
            .setHost(HOST_NAME)
            .build();

    private static final JsonTemplateLayout JSON_TEMPLATE_GELF_LAYOUT = JsonTemplateLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setEventTemplateUri("classpath:GelfLayout.json")
            .setEventDelimiter("\0")
            .setEventTemplateAdditionalFields(new EventTemplateAdditionalField[] {
                EventTemplateAdditionalField.newBuilder()
                        .setKey("host")
                        .setValue(HOST_NAME)
                        .build()
            })
            .build();

    // Note that EcsLayout doesn't support charset configuration, though it uses
    // UTF-8 internally.
    private static final EcsLayout ECS_LAYOUT = EcsLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setServiceName(SERVICE_NAME)
            .setEventDataset(EVENT_DATASET)
            .build();

    private static final JsonTemplateLayout JSON_TEMPLATE_ECS_LAYOUT = JsonTemplateLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setEventTemplateUri("classpath:EcsLayout.json")
            .setRecyclerFactory(ThreadLocalRecyclerFactory.getInstance())
            .setEventTemplateAdditionalFields(new EventTemplateAdditionalField[] {
                EventTemplateAdditionalField.newBuilder()
                        .setKey("service.name")
                        .setValue(SERVICE_NAME)
                        .build(),
                EventTemplateAdditionalField.newBuilder()
                        .setKey("event.dataset")
                        .setValue(EVENT_DATASET)
                        .build()
            })
            .build();

    private static final int LOG_EVENT_COUNT = 100;

    private static final String ES_INDEX_MESSAGE_FIELD_NAME = "message";

    private static RestClient REST_CLIENT;

    private static ElasticsearchTransport ES_TRANSPORT;

    private static ElasticsearchClient ES_CLIENT;

    /**
     * Constants hardcoded in docker-maven-plugin configuration, do not change!
     */
    private static final class MavenHardcodedConstants {

        private MavenHardcodedConstants() {}

        private static final int LS_GELF_INPUT_PORT = 12222;

        private static final int LS_TCP_INPUT_PORT = 12345;

        private static final int ES_PORT = 9200;

        private static final String ES_INDEX_NAME = "log4j";
    }

    @BeforeAll
    public static void initClient() throws IOException {

        LOGGER.info("instantiating the ES client");
        REST_CLIENT = RestClient.builder(
                        HttpHost.create(String.format("http://%s:%d", HOST_NAME, MavenHardcodedConstants.ES_PORT)))
                .build();
        ES_TRANSPORT = new RestClientTransport(REST_CLIENT, new JacksonJsonpMapper());
        ES_CLIENT = new ElasticsearchClient(ES_TRANSPORT);

        LOGGER.info("verifying the ES connection");
        HealthResponse healthResponse = ES_CLIENT.cluster().health();
        Assertions.assertThat(healthResponse.status()).isNotEqualTo(HealthStatus.Red);
    }

    @BeforeEach
    void deleteIndex() throws IOException {
        LOGGER.info("deleting the ES index");
        try {
            DeleteIndexResponse deleteIndexResponse = ES_CLIENT
                    .indices()
                    .delete(DeleteIndexRequest.of(builder -> builder.index(MavenHardcodedConstants.ES_INDEX_NAME)));
            Assertions.assertThat(deleteIndexResponse.acknowledged()).isTrue();
        } catch (ElasticsearchException error) {
            if (!error.getMessage().contains("index_not_found_exception")) {
                throw new RuntimeException(error);
            }
        }
    }

    @AfterAll
    public static void stopClient() throws Exception {
        ES_TRANSPORT.close();
        REST_CLIENT.close();
    }

    @Test
    void test_lite_events() throws IOException {
        final List<LogEvent> logEvents = LogEventFixture.createLiteLogEvents(LOG_EVENT_COUNT);
        testEvents(logEvents);
    }

    @Test
    void test_full_events() throws IOException {
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);
        testEvents(logEvents);
    }

    private static void testEvents(final List<LogEvent> logEvents) throws IOException {
        final Appender appender =
                createStartedAppender(JSON_TEMPLATE_GELF_LAYOUT, MavenHardcodedConstants.LS_GELF_INPUT_PORT);
        try {

            // Append events.
            LOGGER.info("appending events");
            logEvents.forEach(appender::append);
            LOGGER.info("completed appending events");

            // Wait all messages to arrive.
            Awaitility.await()
                    .atMost(Duration.ofSeconds(60))
                    .pollDelay(Duration.ofSeconds(2))
                    .until(() -> checkDocumentCount(LOG_EVENT_COUNT));

            // Verify indexed messages.
            final Set<String> expectedMessages = logEvents.stream()
                    .map(LogstashIT::expectedLogstashMessageField)
                    .collect(Collectors.toSet());
            final Set<String> actualMessages = queryDocuments().stream()
                    .map(source -> (String) source.get(ES_INDEX_MESSAGE_FIELD_NAME))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Assertions.assertThat(actualMessages).isEqualTo(expectedMessages);

        } finally {
            appender.stop();
        }
    }

    private static String expectedLogstashMessageField(final LogEvent logEvent) {
        final Throwable throwable = logEvent.getThrown();
        if (throwable != null) {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    final PrintStream printStream = new PrintStream(outputStream, false, CHARSET.name())) {
                throwable.printStackTrace(printStream);
                return outputStream.toString(CHARSET.name());
            } catch (final Exception error) {
                throw new RuntimeException("failed printing stack trace", error);
            }
        } else {
            return logEvent.getMessage().getFormattedMessage();
        }
    }

    @Test
    void test_newlines() throws IOException {

        // Create two log events containing new lines.
        final Level level = Level.DEBUG;
        final String loggerFqcn = "f.q.c.n";
        final String loggerName = "A";
        final SimpleMessage message1 = new SimpleMessage("line1\nline2\r\nline3");
        final long instantMillis1 = Instant.EPOCH.toEpochMilli();
        final LogEvent logEvent1 = Log4jLogEvent.newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message1)
                .setTimeMillis(instantMillis1)
                .build();
        final SimpleMessage message2 = new SimpleMessage("line4\nline5\r\nline6");
        final long instantMillis2 = instantMillis1 + Duration.ofDays(1).toMillis();
        final LogEvent logEvent2 = Log4jLogEvent.newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message2)
                .setTimeMillis(instantMillis2)
                .build();

        final Appender appender =
                createStartedAppender(JSON_TEMPLATE_GELF_LAYOUT, MavenHardcodedConstants.LS_GELF_INPUT_PORT);
        try {

            // Append the event.
            LOGGER.info("appending events");
            appender.append(logEvent1);
            appender.append(logEvent2);
            LOGGER.info("completed appending events");

            // Wait the message to arrive.
            Awaitility.await()
                    .atMost(Duration.ofSeconds(60))
                    .pollDelay(Duration.ofSeconds(2))
                    .until(() -> checkDocumentCount(2));

            // Verify indexed messages.
            final Set<String> expectedMessages = Stream.of(logEvent1, logEvent2)
                    .map(LogstashIT::expectedLogstashMessageField)
                    .collect(Collectors.toSet());
            final Set<String> actualMessages = queryDocuments().stream()
                    .map(source -> (String) source.get(ES_INDEX_MESSAGE_FIELD_NAME))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Assertions.assertThat(actualMessages).isEqualTo(expectedMessages);

        } finally {
            appender.stop();
        }
    }

    @Test
    void test_GelfLayout() throws IOException {

        // Create log events.
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);

        // Create a function to uniquely identify each document
        final Function<Map<String, Object>, Integer> keyMapper = (final Map<String, Object> source) -> {
            final String timestamp = (String) source.get("timestamp");
            final String shortMessage = (String) source.get("short_message");
            final String fullMessage = (String) source.get("full_message");
            return Objects.hash(timestamp, shortMessage, fullMessage);
        };

        // Collect documents created by `GelfLayout`
        final Map<Integer, Object> expectedSourceByKey = appendAndCollect(
                logEvents, GELF_LAYOUT, MavenHardcodedConstants.LS_GELF_INPUT_PORT, keyMapper, Collections.emptySet());

        // Reset the index
        deleteIndex();

        // Collect documents created by `JsonTemplateLayout`
        final Map<Integer, Object> actualSourceByKey = appendAndCollect(
                logEvents,
                JSON_TEMPLATE_GELF_LAYOUT,
                MavenHardcodedConstants.LS_GELF_INPUT_PORT,
                keyMapper,
                Collections.emptySet());

        // Compare persisted sources.
        Assertions.assertThat(actualSourceByKey).isEqualTo(expectedSourceByKey);
    }

    @Test
    void test_EcsLayout() throws IOException {

        // Create log events.
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);

        // Create a function to uniquely identify each document
        final Function<Map<String, Object>, Integer> keyMapper = (final Map<String, Object> source) -> {
            final String timestamp = (String) source.get("@timestamp");
            final String message = (String) source.get("message");
            final String errorMessage = (String) source.get("error.message");
            return Objects.hash(timestamp, message, errorMessage);
        };

        // Collect documents created by `EcsLayout`
        final Set<String> excludedKeys = Collections.singleton("port");
        final Map<Integer, Object> expectedSourceByKey = appendAndCollect(
                logEvents, ECS_LAYOUT, MavenHardcodedConstants.LS_TCP_INPUT_PORT, keyMapper, excludedKeys);

        // Reset the index
        deleteIndex();

        // Collect documents created by `JsonTemplateLayout`
        final Map<Integer, Object> actualSourceByKey = appendAndCollect(
                logEvents,
                JSON_TEMPLATE_ECS_LAYOUT,
                MavenHardcodedConstants.LS_TCP_INPUT_PORT,
                keyMapper,
                excludedKeys);

        // Compare persisted sources.
        Assertions.assertThat(actualSourceByKey).isEqualTo(expectedSourceByKey);
    }

    private static <K> Map<K, Object> appendAndCollect(
            final List<LogEvent> logEvents,
            final Layout<?> layout,
            final int port,
            final Function<Map<String, Object>, K> keyMapper,
            final Set<String> excludedKeys)
            throws IOException {
        final Appender appender = createStartedAppender(layout, port);
        try {

            // Append the event.
            LOGGER.info("appending events");
            logEvents.forEach(appender::append);
            LOGGER.info("completed appending events");

            // Wait the message to arrive.
            Awaitility.await()
                    .atMost(Duration.ofSeconds(60))
                    .pollDelay(Duration.ofSeconds(2))
                    .until(() -> checkDocumentCount(LOG_EVENT_COUNT));

            // Retrieve the persisted messages.
            return queryDocuments().stream().collect(Collectors.toMap(keyMapper, (final Map<String, Object> source) -> {
                excludedKeys.forEach(source::remove);
                return source;
            }));

        } finally {
            appender.stop();
        }
    }

    private static SocketAppender createStartedAppender(final Layout<?> layout, final int port) {
        LOGGER.info("creating the appender");
        final SocketAppender appender = SocketAppender.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setHost(HOST_NAME)
                .setPort(port)
                .setReconnectDelayMillis(100)
                .setName("LogstashItAppender")
                .setBufferedIo(false)
                .setImmediateFail(true)
                .setIgnoreExceptions(false)
                .setLayout(layout)
                .build();
        appender.start();
        return appender;
    }

    private static boolean checkDocumentCount(int expectedCount) throws IOException {
        final CountResponse countResponse =
                ES_CLIENT.count(builder -> builder.index(MavenHardcodedConstants.ES_INDEX_NAME));
        final long actualCount = countResponse.count();
        Assertions.assertThat(actualCount).isLessThanOrEqualTo(expectedCount);
        return actualCount == expectedCount;
    }

    private static List<Map<String, Object>> queryDocuments() throws IOException {
        @SuppressWarnings("rawtypes")
        SearchResponse<Map> searchResponse = ES_CLIENT.search(
                searchBuilder -> searchBuilder
                        .index(MavenHardcodedConstants.ES_INDEX_NAME)
                        .size(LOG_EVENT_COUNT)
                        .source(SourceConfig.of(sourceConfigBuilder -> sourceConfigBuilder.fetch(true))),
                Map.class);
        return searchResponse.hits().hits().stream()
                .map(hit -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> source = hit.source();
                    return source;
                })
                .collect(Collectors.toList());
    }
}
