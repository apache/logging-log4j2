package org.apache.logging.log4j.layout.template.json.util;

import org.apache.logging.log4j.core.time.MutableInstant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.TimeZone;

class InstantFormatterTest {

    @ParameterizedTest
    @CsvSource({
            "yyyy-MM-dd'T'HH:mm:ss.SSS"             + ",FixedDateFormat",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"          + ",FastDateFormat",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'"    + ",DateTimeFormatter"
    })
    void all_internal_implementations_should_be_used(
            final String pattern,
            final String className) {
        final InstantFormatter formatter = InstantFormatter
                .newBuilder()
                .setPattern(pattern)
                .build();
        Assertions
                .assertThat(formatter.getInternalImplementationClass())
                .asString()
                .describedAs("pattern=%s", pattern)
                .endsWith("." + className);
    }

    @Test
    void nanoseconds_should_be_formatted() {
        final InstantFormatter formatter = InstantFormatter
                .newBuilder()
                .setPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                .setTimeZone(TimeZone.getTimeZone("UTC"))
                .build();
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(0, 123_456_789);
        Assertions
                .assertThat(formatter.format(instant))
                .isEqualTo("1970-01-01T00:00:00.123456789Z");
    }

}
