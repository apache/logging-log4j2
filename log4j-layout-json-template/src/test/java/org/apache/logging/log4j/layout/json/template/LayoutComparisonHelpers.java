package org.apache.logging.log4j.layout.json.template;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

import java.util.LinkedHashMap;
import java.util.Map;

enum LayoutComparisonHelpers {;

    static Map<String, Object> removeNullEntries(final Map<String, Object> root) {
        final Map<String, Object> trimmedRoot = new LinkedHashMap<>(root);
        root.forEach((final String key, final Object value) -> {
            if (value == null) {
                trimmedRoot.remove(key);
            }
        });
        return trimmedRoot;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> renderUsing(
            final LogEvent logEvent,
            final Layout<String> layout)
            throws Exception {
        final String json = layout.toSerializable(logEvent);
        return JacksonFixture.getObjectMapper().readValue(json, Map.class);
    }

}
