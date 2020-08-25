package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;

import java.util.Map;

enum LayoutComparisonHelpers {;

    @SuppressWarnings("unchecked")
    static Map<String, Object> renderUsing(
            final LogEvent logEvent,
            final Layout<String> layout) {
        final String json = layout.toSerializable(logEvent);
        return (Map<String, Object>) JsonReader.read(json);
    }

}
