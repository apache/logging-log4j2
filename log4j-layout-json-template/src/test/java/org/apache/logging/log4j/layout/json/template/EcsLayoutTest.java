package org.apache.logging.log4j.layout.json.template;

import co.elastic.logging.log4j2.EcsLayout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.layout.json.template.LayoutComparisonHelpers.renderUsing;

public class EcsLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final JsonTemplateLayout JSON_TEMPLATE_LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setEventTemplateUri("classpath:EcsLayout.json")
            .setEventTemplateAdditionalFields(
                    JsonTemplateLayout
                            .EventTemplateAdditionalFields
                            .newBuilder()
                            .setAdditionalFields(
                                    new KeyValuePair[]{
                                            new KeyValuePair(
                                                    "service.name",
                                                    "test")
                                    })
                            .build())
            .build();

    private static final EcsLayout ECS_LAYOUT = EcsLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setServiceName("test")
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
        final Map<String, Object> ecsLayoutMap = renderUsingEcsLayout(logEvent);
        Assertions.assertThat(jsonTemplateLayoutMap).isEqualTo(ecsLayoutMap);
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(
            final LogEvent logEvent)
            throws Exception {
        return renderUsing(logEvent, JSON_TEMPLATE_LAYOUT);
    }

    private static Map<String, Object> renderUsingEcsLayout(
            final LogEvent logEvent)
            throws Exception {
        return renderUsing(logEvent, ECS_LAYOUT);
    }

}
