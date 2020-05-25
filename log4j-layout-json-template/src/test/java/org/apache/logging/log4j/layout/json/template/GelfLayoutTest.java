package org.apache.logging.log4j.layout.json.template;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.layout.json.template.LayoutComparisonHelpers.renderUsing;

public class GelfLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final String HOST_NAME = "localhost";

    private static final JsonTemplateLayout JSON_TEMPLATE_LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplateUri("classpath:GelfLayout.json")
            .setEventTemplateAdditionalFields(
                    JsonTemplateLayout
                            .EventTemplateAdditionalFields
                            .newBuilder()
                            .setAdditionalFields(
                                    new KeyValuePair[]{
                                            new KeyValuePair(
                                                    "host",
                                                    HOST_NAME)
                                    })
                            .build())
            .build();

    private static final GelfLayout GELF_LAYOUT = GelfLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setHost(HOST_NAME)
            .setCompressionType(GelfLayout.CompressionType.OFF)
            .build();

    @Test
    public void test_lite_log_events() throws Exception {
        final List<LogEvent> logEvents = LogEventFixture.createLiteLogEvents(1_000);
        test(logEvents);
    }

    @Test
    public void test_full_log_events() throws Exception {
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(1_000);
        test(logEvents);
    }

    private static void test(final Collection<LogEvent> logEvents) throws Exception {
        for (final LogEvent logEvent : logEvents) {
            test(logEvent);
        }
    }

    private static void test(final LogEvent logEvent) throws Exception {
        final Map<String, Object> jsonTemplateLayoutMap = renderUsingJsonTemplateLayout(logEvent);
        final Map<String, Object> gelfLayoutMap = renderUsingGelfLayout(logEvent);
        verifyTimestamp(jsonTemplateLayoutMap, gelfLayoutMap);
        Assertions.assertThat(jsonTemplateLayoutMap).isEqualTo(gelfLayoutMap);
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(
            final LogEvent logEvent)
            throws Exception {
        return renderUsing(logEvent, JSON_TEMPLATE_LAYOUT);
    }

    private static Map<String, Object> renderUsingGelfLayout(
            final LogEvent logEvent)
            throws Exception {
        return renderUsing(logEvent, GELF_LAYOUT);
    }

    /**
     * Handle timestamps individually to avoid floating-point comparison hiccups.
     */
    private static void verifyTimestamp(
            final Map<String, Object> jsonTemplateLayoutMap,
            final Map<String, Object> gelfLayoutMap) {
        final Number jsonTemplateLayoutTimestamp =
                (Number) jsonTemplateLayoutMap.remove("timestamp");
        final Number gelfLayoutTimestamp =
                (Number) gelfLayoutMap.remove("timestamp");
        Assertions
                .assertThat(jsonTemplateLayoutTimestamp.doubleValue())
                .isCloseTo(
                        gelfLayoutTimestamp.doubleValue(),
                        Percentage.withPercentage(0.01));
    }

}
