package org.apache.logging.log4j.layout.json.template;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.jackson.json.layout.JsonLayout;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        final Map<String, Object> jsonLayoutMap = renderUsingJsonLayout(logEvent);
        Assertions.assertThat(jsonTemplateLayoutMap).isEqualTo(jsonLayoutMap);
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(
            final LogEvent logEvent)
            throws Exception {
        final Map<String, Object> map = renderUsing(logEvent, JSON_TEMPLATE_LAYOUT);
        final Map<String, Object> emptySourceExcludedMap = removeEmptyObject(map, "source");
        // JsonLayout blindly serializes the Throwable as a POJO, this is,
        // to say the least, quite wrong, and I ain't gonna try to emulate
        // this behaviour in JsonTemplateLayout. Hence, discarding the "thrown"
        // field.
        emptySourceExcludedMap.remove("thrown");
        return emptySourceExcludedMap;
    }

    private static Map<String, Object> renderUsingJsonLayout(
            final LogEvent logEvent)
            throws Exception {
        final Map<String, Object> map = renderUsing(logEvent, JSON_LAYOUT);
        // JsonLayout blindly serializes the Throwable as a POJO, this is,
        // to say the least, quite wrong, and I ain't gonna try to emulate
        // this behaviour in JsonTemplateLayout. Hence, discarding the "thrown"
        // field.
        map.remove("thrown");
        return map;
    }

    private static Map<String, Object> removeEmptyObject(
            final Map<String, Object> root,
            final String key) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> source =
                (Map<String, Object>) root.getOrDefault(
                        key, Collections.emptyMap());
        boolean emptySource = source
                .values()
                .stream()
                .allMatch(Objects::isNull);
        if (!emptySource) {
            return root;
        }
        final Map<String, Object> trimmedRoot = new LinkedHashMap<>(root);
        trimmedRoot.remove(key);
        return trimmedRoot;
    }

}
