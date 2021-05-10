package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.MapAccessor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class TestHelpers {

    public static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final JsonWriter JSON_WRITER = JsonWriter
            .newBuilder()
            .setMaxStringLength(10_000)
            .setTruncatedStringSuffix("…")
            .build();

    private TestHelpers() {}

    @SuppressWarnings("unchecked")
    static Map<String, Object> serializeUsingLayout(
            final LogEvent logEvent,
            final Layout<String> layout) {
        final String json = layout.toSerializable(logEvent);
        return (Map<String, Object>) JsonReader.read(json);
    }

    public static synchronized String writeJson(final Object value) {
        final StringBuilder stringBuilder = JSON_WRITER.getStringBuilder();
        stringBuilder.setLength(0);
        try {
            JSON_WRITER.writeValue(value);
            return stringBuilder.toString();
        } finally {
            stringBuilder.setLength(0);
        }
    }

    public static void usingSerializedLogEventAccessor(
            final Layout<String> layout,
            final LogEvent logEvent,
            final Consumer<MapAccessor> accessorConsumer) {
        final String serializedLogEventJson = layout.toSerializable(logEvent);
        @SuppressWarnings("unchecked")
        final Map<String, Object> deserializedLogEvent =
                (Map<String, Object>) readJson(serializedLogEventJson);
        final MapAccessor serializedLogEventAccessor = new MapAccessor(deserializedLogEvent);
        accessorConsumer.accept(serializedLogEventAccessor);
    }

    public static Object readJson(final String json) {
        return JsonReader.read(json);
    }

    public static Map<String, Object> asMap(final Object... pairs) {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("odd number of arguments: " + pairs.length);
        }
        for (int i = 0; i < pairs.length; i += 2) {
            final String key = (String) pairs[i];
            final Object value = pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

}
