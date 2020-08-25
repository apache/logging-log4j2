/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json.resolver;

/**
 * Exception resolver factory.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config      = field , [ stringified ]
 * field       = "field" -> ( "className" | "message" | "stackTrace" )
 * stringified = "stringified" -> boolean
 * </pre>
 */
abstract class ExceptionInternalResolverFactory {

    private static final EventResolver NULL_RESOLVER =
            (ignored, jsonGenerator) -> jsonGenerator.writeNull();

    EventResolver createInternalResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");
        switch (fieldName) {
            case "className": return createClassNameResolver();
            case "message": return createMessageResolver(context);
            case "stackTrace": return createStackTraceResolver(context, config);
        }
        throw new IllegalArgumentException("unknown field: " + config);

    }

    abstract EventResolver createClassNameResolver();

    abstract EventResolver createMessageResolver(EventResolverContext context);

    private EventResolver createStackTraceResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        if (!context.isStackTraceEnabled()) {
            return NULL_RESOLVER;
        }
        final boolean stringified = config.getBoolean("stringified", false);
        return stringified
                ? createStackTraceStringResolver(context)
                : createStackTraceObjectResolver(context);
    }

    abstract EventResolver createStackTraceStringResolver(EventResolverContext context);

    abstract EventResolver createStackTraceObjectResolver(EventResolverContext context);

}
