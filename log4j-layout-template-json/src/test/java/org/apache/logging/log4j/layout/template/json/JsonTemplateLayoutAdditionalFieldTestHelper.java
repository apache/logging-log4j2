package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.assertj.core.api.Assertions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class JsonTemplateLayoutAdditionalFieldTestHelper {

    static void assertAdditionalFields(final LoggerContext loggerContext, ListAppender appender) {
        // Log an event.
        final Logger logger =
                loggerContext.getLogger(
                        JsonTemplateLayoutAdditionalFieldTestHelper.class);
        logger.info("trigger");

        // Verify that the appender has logged the event.
        final List<byte[]> serializedEvents = appender.getData();
        Assertions.assertThat(serializedEvents).hasSize(1);

        // Deserialize the serialized event.
        final byte[] serializedEvent = serializedEvents.get(0);
        final String serializedEventJson =
                new String(
                        serializedEvent,
                        JsonTemplateLayoutDefaults.getCharset());
        final Object serializedEventObject = JsonReader.read(serializedEventJson);
        Assertions.assertThat(serializedEventObject).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked") final Map<String, Object> serializedEventMap =
                (Map<String, Object>) serializedEventObject;

        // Verify the serialized additional fields.
        Assertions
                .assertThat(serializedEventMap)
                .containsEntry("stringField", "string")
                .containsEntry("numberField", 1)
                .containsEntry("objectField", Collections.singletonMap("numberField", 1))
                .containsEntry("listField", Arrays.asList(1, "two"));

    }
}
