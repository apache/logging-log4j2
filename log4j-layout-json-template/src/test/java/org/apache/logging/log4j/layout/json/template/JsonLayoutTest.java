package org.apache.logging.log4j.layout.json.template;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.jackson.json.layout.JsonLayout;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.layout.json.template.LayoutComparisonHelpers.renderUsing;

public class JsonLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final JsonTemplateLayout JSON_TEMPLATE_LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplateUri("classpath:JsonLayout.json")
            .build();

    private static final JsonLayout JSON_LAYOUT = JsonLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .build();

    @Test
    public void test_lite_log_events() {
        final List<LogEvent> logEvents = LogEventFixture.createLiteLogEvents(1_000);
        test(logEvents);
    }

    @Test
    public void test_full_log_events() {
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(1_000);
        test(logEvents);
    }

    private static void test(final Collection<LogEvent> logEvents) {
        for (final LogEvent logEvent : logEvents) {
            test(logEvent);
        }
    }

    private static void test(final LogEvent logEvent) {
        final Map<String, Object> jsonTemplateLayoutMap = renderUsingJsonTemplateLayout(logEvent);
        final Map<String, Object> jsonLayoutMap = renderUsingJsonLayout(logEvent);
        // JsonLayout blindly serializes the Throwable as a POJO, this is,
        // to say the least, quite wrong, and I ain't gonna try to emulate
        // this behaviour in JsonTemplateLayout. Hence, discarding the "thrown"
        // field.
        jsonTemplateLayoutMap.remove("thrown");
        jsonLayoutMap.remove("thrown");
        Assertions.assertThat(jsonTemplateLayoutMap).isEqualTo(jsonLayoutMap);
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(
            final LogEvent logEvent) {
        return renderUsing(logEvent, JSON_TEMPLATE_LAYOUT);
    }

    private static Map<String, Object> renderUsingJsonLayout(
            final LogEvent logEvent) {
        return renderUsing(logEvent, JSON_LAYOUT);
    }

}
