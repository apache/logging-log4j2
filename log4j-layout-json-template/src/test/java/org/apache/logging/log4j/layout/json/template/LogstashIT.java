package org.apache.logging.log4j.layout.json.template;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
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
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogstashIT {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final DefaultConfiguration CONFIGURATION = new DefaultConfiguration();

    private static final int LOG_EVENT_COUNT = 100;

    private static final String HOST_NAME = "localhost";

    private static final String ES_INDEX_MESSAGE_FIELD_NAME = "message";

    /**
     * Constants hardcoded in docker-maven-plugin configuration, do not change!
     */
    private enum MavenHardcodedConstants {;

        private static final int LS_TCP_PLUGIN_PORT = 12345;

        private static final int ES_PORT = 9200;

        private static final String ES_INDEX_NAME = "log4j";

    }

    @Test
    public void test_lite_events() throws IOException {
        final List<LogEvent> logEvents =
                LogEventFixture.createLiteLogEvents(LOG_EVENT_COUNT);
        testEvents(logEvents);
    }

    @Test
    public void test_full_events() throws IOException {
        final List<LogEvent> logEvents =
                LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);
        testEvents(logEvents);
    }

    private static void testEvents(final List<LogEvent> logEvents) throws IOException {
        try (final RestHighLevelClient client = createClient()) {
            final Appender appender = createStartedAppender();
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
                        .map(LogEvent::getMessage)
                        .map(Message::getFormattedMessage)
                        .collect(Collectors.toSet());
                final Set<String> actualMessages =
                        new HashSet<>(queryDocumentMessages(client));
                Assertions
                        .assertThat(actualMessages)
                        .isEqualTo(expectedMessages);

            } finally {
                appender.stop();
            }
        }
    }

    @Test
    public void test_newlines() throws IOException {

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

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate("{\"message\":\"${json:message}\"}")
                .build();

        try (final RestHighLevelClient client = createClient()) {
            final Appender appender = createStartedAppender(layout);
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
                        .of(message1, message2)
                        .map(Message::getFormattedMessage)
                        .collect(Collectors.toSet());
                final Set<String> actualMessages =
                        new HashSet<>(queryDocumentMessages(client));
                Assertions
                        .assertThat(actualMessages)
                        .isEqualTo(expectedMessages);

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

    private static SocketAppender createStartedAppender() {
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:EcsLayout.json")
                .build();
        return createStartedAppender(layout);
    }

    private static SocketAppender createStartedAppender(final JsonTemplateLayout layout) {
        LOGGER.info("creating the appender");
        final SocketAppender appender = SocketAppender
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setHost(HOST_NAME)
                .setPort(MavenHardcodedConstants.LS_TCP_PLUGIN_PORT)
                .setReconnectDelayMillis(100)
                .setName("LogstashIT")
                .setBufferedIo(false)
                .setImmediateFail(true)
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

    private static List<String> queryDocumentMessages(
            final RestHighLevelClient client
    ) throws IOException {
        final SearchSourceBuilder searchSourceBuilder =
                new SearchSourceBuilder()
                        .size(LOG_EVENT_COUNT)
                        .fetchSource(new String[]{ES_INDEX_MESSAGE_FIELD_NAME}, null);
        final SearchRequest searchRequest =
                new SearchRequest(MavenHardcodedConstants.ES_INDEX_NAME)
                        .source(searchSourceBuilder);
        try {
            final SearchResponse searchResponse =
                    client.search(searchRequest, RequestOptions.DEFAULT);
            return Arrays
                    .stream(searchResponse.getHits().getHits())
                    .map(SearchHit::getSourceAsMap)
                    .filter(Objects::nonNull)
                    .map(map -> (String) map.get(ES_INDEX_MESSAGE_FIELD_NAME))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (ElasticsearchStatusException error) {
            if (RestStatus.NOT_FOUND.equals(error.status())) {
                return Collections.emptyList();
            }
            throw new IOException(error);
        }
    }

}
