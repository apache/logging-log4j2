package org.apache.logging.log4j.layout.json.template;

import co.elastic.logging.log4j2.EcsLayout;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(1_000);
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
        final Map<String, Object> map = renderUsing(logEvent, JSON_TEMPLATE_LAYOUT);
        return stringifyGroup(map, "labels");
    }

    @SuppressWarnings("SameParameterValue")
    private static Map<String, Object> stringifyGroup(
            final Map<String, Object> input,
            final String key) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> subInput = (Map<String, Object>) input.get(key);
        if (subInput == null) {
            return input;
        } else {
            final Map<String, Object> subOutput = new LinkedHashMap<>();
            subInput.forEach((final String subKey, final Object subValue) ->
                    subOutput.put(subKey, String.valueOf(subValue)));
            final Map<String, Object> output = new LinkedHashMap<>(input);
            output.put(key, subOutput);
            return output;
        }
    }

    private static Map<String, Object> renderUsingEcsLayout(
            final LogEvent logEvent)
            throws Exception {
        final Map<String, Object> map = renderUsing(logEvent, ECS_LAYOUT);
        return groupKeys(map, "labels");
    }

    @SuppressWarnings("SameParameterValue")
    private static Map<String, Object> groupKeys(
            final Map<String, Object> input,
            final String prefix) {
        final Map<String, Object> output = new LinkedHashMap<>();
        input.forEach((final String key, final Object value) -> {
            if (key.startsWith(prefix + '.')) {
                final String subKey = key.replaceFirst("^" + prefix + "\\.", "");
                @SuppressWarnings("unchecked")
                final Map<String, Object> subOutput =
                        (Map<String, Object>) output.computeIfAbsent(
                                prefix,
                                ignored -> new LinkedHashMap<>());
                subOutput.put(subKey, value);
            } else {
                output.put(key, value);
            }
        });
        return output;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> renderUsing(
            final LogEvent logEvent,
            final Layout<String> layout)
            throws Exception {
        final String json = layout.toSerializable(logEvent);
        return JacksonFixture.getObjectMapper().readValue(json, Map.class);
    }

}
