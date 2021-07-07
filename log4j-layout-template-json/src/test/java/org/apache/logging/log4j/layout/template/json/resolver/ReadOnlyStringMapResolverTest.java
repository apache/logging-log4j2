package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.PatternSyntaxException;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReadOnlyStringMapResolverTest {

    @Test
    void key_should_not_be_allowed_with_flatten() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "key", "foo",
                        "flatten", true)),
                IllegalArgumentException.class,
                "key and flatten options cannot be combined");
    }

    @Test
    void invalid_pattern_should_fail() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "pattern", "[1")),
                PatternSyntaxException.class,
                "Unclosed character");
    }

    @Test
    void pattern_should_not_be_allowed_with_key() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "key", "foo",
                        "pattern", "bar")),
                IllegalArgumentException.class,
                "pattern and key options cannot be combined");
    }

    @Test
    void replacement_should_not_be_allowed_without_pattern() {
        verifyConfigFailure(
                writeJson(asMap(
                        "$resolver", "mdc",
                        "replacement", "$1")),
                IllegalArgumentException.class,
                "replacement cannot be provided without a pattern");
    }

    private void verifyConfigFailure(
            final String eventTemplate,
            final Class<? extends Throwable> failureClass,
            final String failureMessage) {
        Assertions
                .assertThatThrownBy(() -> JsonTemplateLayout
                        .newBuilder()
                        .setConfiguration(CONFIGURATION)
                        .setEventTemplate(eventTemplate)
                        .build())
                .isInstanceOf(failureClass)
                .hasMessageContaining(failureMessage);
    }

    @Test
    void pattern_replacement_should_work() {

        // Create the event template.
        final String eventTemplate = writeJson(asMap(
                "$resolver", "mdc",
                "pattern", "user:(role|rank)",
                "replacement", "$1"));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("user:role", "engineer");
        contextData.putValue("user:rank", "senior");
        final LogEvent logEvent = Log4jLogEvent
                .newBuilder()
                .setContextData(contextData)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> {
            assertThat(accessor.getString("role")).isEqualTo("engineer");
            assertThat(accessor.getString("rank")).isEqualTo("senior");
        });

    }

}
