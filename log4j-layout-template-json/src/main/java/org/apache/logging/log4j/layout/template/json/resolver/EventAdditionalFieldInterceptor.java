/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.layout.template.json.resolver;

import java.util.Map;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;

/**
 * Interceptor to add {@link EventTemplateAdditionalField
 * additional fields} after event template read.
 */
@Plugin(name = "EventAdditionalFieldInterceptor", category = TemplateResolverInterceptor.CATEGORY)
public class EventAdditionalFieldInterceptor implements EventResolverInterceptor {

    private static final EventAdditionalFieldInterceptor INSTANCE = new EventAdditionalFieldInterceptor();

    private EventAdditionalFieldInterceptor() {}

    @PluginFactory
    public static EventAdditionalFieldInterceptor getInstance() {
        return INSTANCE;
    }

    @Override
    public Object processTemplateBeforeResolverInjection(final EventResolverContext context, final Object node) {

        // Short-circuit if there are no additional fields.
        final EventTemplateAdditionalField[] additionalFields = context.getEventTemplateAdditionalFields();
        if (additionalFields.length == 0) {
            return node;
        }

        // Check that the root is an object node.
        final Map<String, Object> objectNode;
        try {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) node;
            objectNode = map;
        } catch (final ClassCastException error) {
            final String message = String.format(
                    "was expecting an object to merge additional fields: %s",
                    node.getClass().getName());
            throw new IllegalArgumentException(message);
        }

        // Merge additional fields.
        for (final EventTemplateAdditionalField additionalField : additionalFields) {
            final String additionalFieldKey = additionalField.getKey();
            final Object additionalFieldValue;
            final EventTemplateAdditionalField.Format additionalFieldFormat = additionalField.getFormat();
            if (EventTemplateAdditionalField.Format.STRING.equals(additionalFieldFormat)) {
                additionalFieldValue = additionalField.getValue();
            } else if (EventTemplateAdditionalField.Format.JSON.equals(additionalFieldFormat)) {
                try {
                    additionalFieldValue = JsonReader.read(additionalField.getValue());
                } catch (final Exception error) {
                    final String message =
                            String.format("failed reading JSON provided by additional field: %s", additionalFieldKey);
                    throw new IllegalArgumentException(message, error);
                }
            } else {
                final String message = String.format(
                        "unknown format %s for additional field: %s", additionalFieldKey, additionalFieldFormat);
                throw new IllegalArgumentException(message);
            }
            objectNode.put(additionalFieldKey, additionalFieldValue);
        }

        // Return the modified node.
        return node;
    }
}
