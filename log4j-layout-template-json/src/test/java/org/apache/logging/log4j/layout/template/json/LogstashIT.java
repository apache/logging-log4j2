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
package org.apache.logging.log4j.layout.template.json;

import co.elastic.logging.log4j2.EcsLayout;
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
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Execution(ExecutionMode.SAME_THREAD)
class LogstashIT {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final DefaultConfiguration CONFIGURATION = new DefaultConfiguration();

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String HOST_NAME = NetUtils.getLocalHostname();

    private static final String SERVICE_NAME = "LogstashIT";

    private static final String EVENT_DATASET = SERVICE_NAME + ".log";

    private static final GelfLayout GELF_LAYOUT = GelfLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setCompressionType(GelfLayout.CompressionType.OFF)
            .setIncludeNullDelimiter(true)
            .setHost(HOST_NAME)
            .build();

    private static final JsonTemplateLayout JSON_TEMPLATE_GELF_LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setEventTemplateUri("classpath:GelfLayout.json")
            .setEventDelimiter("\0")
            .setEventTemplateAdditionalFields(
                    new EventTemplateAdditionalField[]{
                            EventTemplateAdditionalField
                                    .newBuilder()
                                    .setKey("host")
                                    .setValue(HOST_NAME)
                                    .build()
                    })
            .build();

    // Note that EcsLayout doesn't support charset configuration, though it uses
    // UTF-8 internally.
    private static final EcsLayout ECS_LAYOUT = EcsLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setServiceName(SERVICE_NAME)
            .setEventDataset(EVENT_DATASET)
            .build();

    private static final JsonTemplateLayout JSON_TEMPLATE_ECS_LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setEventTemplateUri("classpath:EcsLayout.json")
            .setRecyclerFactory(ThreadLocalRecyclerFactory.getInstance())
            .setEventTemplateAdditionalFields(
                    new EventTemplateAdditionalField[]{
                            EventTemplateAdditionalField
                                    .newBuilder()
                                    .setKey("service.name")
                                    .setValue(SERVICE_NAME)
                                    .build(),
                            EventTemplateAdditionalField
                                    .newBuilder()
                                    .setKey("event.dataset")
                                    .setValue(EVENT_DATASET)
                                    .build()
                    })
            .build();

    private static final int LOG_EVENT_COUNT = 100;

    private static final String ES_INDEX_MESSAGE_FIELD_NAME = "message";

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

    @Test
    void test_lite_events() throws IOException {
        final List<LogEvent> logEvents =
                LogEventFixture.createLiteLogEvents(LOG_EVENT_COUNT);
        testEvents(logEvents);
    }

    @Test
    void test_full_events() throws IOException {
        final List<LogEvent> logEvents =
                LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);
        testEvents(logEvents);
    }

    private static void testEvents(final List<LogEvent> logEvents) throws IOException {
        try (final RestHighLevelClient client = createClient()) {
            final Appender appender = createStartedAppender(
                    JSON_TEMPLATE_GELF_LAYOUT,
                    MavenHardcodedConstants.LS_GELF_INPUT_PORT);
            try {

                // Append events.
                LOGGER.info("appending events");
                logEvents.forEach(appender::append);
                LOGGER.info("completed appending events");

                // Wait all messages to arrive.
                Awaitility
                        .await()
                        .atMost(Duration.ofSeconds(60))
                        .pollDelay(Duration.ofSeconds(2))
                        .until(() -> queryDocumentCount(client) == LOG_EVENT_COUNT);

                // Verify indexed messages.
                final Set<String> expectedMessages = logEvents
                        .stream()
                        .map(LogstashIT::expectedLogstashMessageField)
                        .collect(Collectors.toSet());
                final Set<String> actualMessages = queryDocuments(client)
                        .stream()
                        .map(source -> (String) source.get(ES_INDEX_MESSAGE_FIELD_NAME))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Assertions
                        .assertThat(actualMessages)
                        .isEqualTo(expectedMessages);

            } finally {
                appender.stop();
            }
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
                throw new RuntimeException(
                        "failed printing stack trace",
                        error);
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
        final LogEvent logEvent1 = Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message1)
                .setTimeMillis(instantMillis1)
                .build();
        final SimpleMessage message2 = new SimpleMessage("line4\nline5\r\nline6");
        final long instantMillis2 = instantMillis1 + Duration.ofDays(1).toMillis();
        final LogEvent logEvent2 = Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message2)
                .setTimeMillis(instantMillis2)
                .build();

        try (final RestHighLevelClient client = createClient()) {
            final Appender appender = createStartedAppender(
                    JSON_TEMPLATE_GELF_LAYOUT,
                    MavenHardcodedConstants.LS_GELF_INPUT_PORT);
            try {

                // Append the event.
                LOGGER.info("appending events");
                appender.append(logEvent1);
                appender.append(logEvent2);
                LOGGER.info("completed appending events");

                // Wait the message to arrive.
                Awaitility
                        .await()
                        .atMost(Duration.ofSeconds(60))
                        .pollDelay(Duration.ofSeconds(2))
                        .until(() -> queryDocumentCount(client) == 2);

                // Verify indexed messages.
                final Set<String> expectedMessages = Stream
                        .of(logEvent1, logEvent2)
                        .map(LogstashIT::expectedLogstashMessageField)
                        .collect(Collectors.toSet());
                final Set<String> actualMessages = queryDocuments(client)
                        .stream()
                        .map(source -> (String) source.get(ES_INDEX_MESSAGE_FIELD_NAME))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Assertions
                        .assertThat(actualMessages)
                        .isEqualTo(expectedMessages);

            } finally {
                appender.stop();
            }
        }

    }

    @Test
    void test_GelfLayout() throws IOException {

        // Create log events.
        final List<LogEvent> logEvents =
                LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);

        // Append log events and collect persisted sources.
        final Function<Map<String, Object>, Integer> keyMapper =
                (final Map<String, Object> source) -> {
                    final String timestamp = (String) source.get("timestamp");
                    final String shortMessage = (String) source.get("short_message");
                    final String fullMessage = (String) source.get("full_message");
                    return Objects.hash(timestamp, shortMessage, fullMessage);
                };
        final Map<Integer, Object> expectedSourceByKey =
                appendAndCollect(
                        logEvents,
                        GELF_LAYOUT,
                        MavenHardcodedConstants.LS_GELF_INPUT_PORT,
                        keyMapper,
                        Collections.emptySet());
        final Map<Integer, Object> actualSourceByKey =
                appendAndCollect(
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
        final List<LogEvent> logEvents =
                LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);

        // Append log events and collect persisted sources.
        final Function<Map<String, Object>, Integer> keyMapper =
                (final Map<String, Object> source) -> {
                    final String timestamp = (String) source.get("@timestamp");
                    final String message = (String) source.get("message");
                    final String errorMessage = (String) source.get("error.message");
                    return Objects.hash(timestamp, message, errorMessage);
                };
        final Set<String> excludedKeys = Collections.singleton("port");
        final Map<Integer, Object> expectedSourceByKey =
                appendAndCollect(
                        logEvents,
                        ECS_LAYOUT,
                        MavenHardcodedConstants.LS_TCP_INPUT_PORT,
                        keyMapper,
                        excludedKeys);
        final Map<Integer, Object> actualSourceByKey =
                appendAndCollect(
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
            final Set<String> excludedKeys) throws IOException {
        try (final RestHighLevelClient client = createClient()) {
            final Appender appender = createStartedAppender(layout, port);
            try {

                // Append the event.
                LOGGER.info("appending events");
                logEvents.forEach(appender::append);
                LOGGER.info("completed appending events");

                // Wait the message to arrive.
                Awaitility
                        .await()
                        .atMost(Duration.ofSeconds(60))
                        .pollDelay(Duration.ofSeconds(2))
                        .until(() -> queryDocumentCount(client) == LOG_EVENT_COUNT);

                // Retrieve the persisted messages.
                return queryDocuments(client)
                        .stream()
                        .collect(Collectors.toMap(
                                keyMapper,
                                (final Map<String, Object> source) -> {
                                    excludedKeys.forEach(source::remove);
                                    return source;
                                }));

            } finally {
                appender.stop();
            }
        }
    }

    private static RestHighLevelClient createClient() throws IOException {

        // Instantiate the client.
        LOGGER.info("instantiating the ES client");
        final HttpHost httpHost = new HttpHost(HOST_NAME, MavenHardcodedConstants.ES_PORT);
        final RestClientBuilder clientBuilder =
                RestClient.builder(httpHost);
        final RestHighLevelClient client = new RestHighLevelClient(clientBuilder);

        // Verify the connection.
        LOGGER.info("verifying the ES connection");
        final ClusterHealthResponse healthResponse = client
                .cluster()
                .health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
        Assertions
                .assertThat(healthResponse.getStatus())
                .isNotEqualTo(ClusterHealthStatus.RED);

        // Delete the index.
        LOGGER.info("deleting the ES index");
        final DeleteIndexRequest deleteRequest =
                new DeleteIndexRequest(MavenHardcodedConstants.ES_INDEX_NAME);
        try {
            final AcknowledgedResponse deleteResponse = client
                    .indices()
                    .delete(deleteRequest, RequestOptions.DEFAULT);
            Assertions
                    .assertThat(deleteResponse.isAcknowledged())
                    .isTrue();
        } catch (ElasticsearchStatusException error) {
            Assertions.assertThat(error)
                    .satisfies(ignored -> Assertions
                            .assertThat(error.status())
                            .isEqualTo(RestStatus.NOT_FOUND));
        }

        return client;

    }

    private static SocketAppender createStartedAppender(
            final Layout<?> layout,
            final int port) {
        LOGGER.info("creating the appender");
        final SocketAppender appender = SocketAppender
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .withHost(HOST_NAME)
                .withPort(port)
                .withReconnectDelayMillis(100)
                .setName("LogstashItAppender")
                .withBufferedIo(false)
                .withImmediateFail(true)
                .setIgnoreExceptions(false)
                .setLayout(layout)
                .build();
        appender.start();
        return appender;
    }

    private static long queryDocumentCount(
            final RestHighLevelClient client)
            throws IOException {
        final SearchSourceBuilder searchSourceBuilder =
                new SearchSourceBuilder()
                        .size(0)
                        .fetchSource(false);
        final SearchRequest searchRequest =
                new SearchRequest(MavenHardcodedConstants.ES_INDEX_NAME)
                        .source(searchSourceBuilder);
        try {
            final SearchResponse searchResponse =
                    client.search(searchRequest, RequestOptions.DEFAULT);
            return searchResponse.getHits().getTotalHits().value;
        } catch (ElasticsearchStatusException error) {
            if (RestStatus.NOT_FOUND.equals(error.status())) {
                return 0L;
            }
            throw new IOException(error);
        }
    }

    private static List<Map<String, Object>> queryDocuments(
            final RestHighLevelClient client
    ) throws IOException {
        final SearchSourceBuilder searchSourceBuilder =
                new SearchSourceBuilder()
                        .size(LOG_EVENT_COUNT)
                        .fetchSource(true);
        final SearchRequest searchRequest =
                new SearchRequest(MavenHardcodedConstants.ES_INDEX_NAME)
                        .source(searchSourceBuilder);
        try {
            final SearchResponse searchResponse =
                    client.search(searchRequest, RequestOptions.DEFAULT);
            return Arrays
                    .stream(searchResponse.getHits().getHits())
                    .map(SearchHit::getSourceAsMap)
                    .collect(Collectors.toList());
        } catch (ElasticsearchStatusException error) {
            if (RestStatus.NOT_FOUND.equals(error.status())) {
                return Collections.emptyList();
            }
            throw new IOException(error);
        }
    }

}
