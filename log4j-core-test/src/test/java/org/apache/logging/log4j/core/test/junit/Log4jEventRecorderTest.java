package org.apache.logging.log4j.core.test.junit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Log4jEventRecorderEnabled
class Log4jEventRecorderTest {

    @Test
    void should_succeed_when_run_even_in_parallel(final Log4jEventRecorder eventRecorder) {

        // Log events
        final int eventCount = 3;//1 + (int) (Math.random() * 1000D);
        final Logger logger = eventRecorder.getLoggerContext().getLogger(Log4jEventRecorderTest.class);
        for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
            logger.trace("test message {}", eventIndex);
        }

        // Verify logged levels
        final List<LogEvent> events = eventRecorder.getEvents();
        assertThat(events).allMatch(event -> Level.TRACE.equals(event.getLevel()));

        // Verify logged messages
        final List<String> expectedMessages = IntStream
                .range(0, eventCount)
                .mapToObj(eventIndex -> String.format("test message %d", eventIndex))
                .collect(Collectors.toList());
        final List<String> actualMessages = events
                .stream()
                .map(event -> event.getMessage().getFormattedMessage())
                .collect(Collectors.toList());
        assertThat(actualMessages).containsExactlyElementsOf(expectedMessages);

    }

}
