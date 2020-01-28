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
package org.apache.logging.log4j.jackson.json.template.layout.resolver;

abstract class ExceptionInternalResolverFactory {

    private static final EventResolver NULL_RESOLVER =
            (ignored, jsonGenerator) -> jsonGenerator.writeNull();

    EventResolver createInternalResolver(
            final EventResolverContext context,
            final String key) {

        // Split the key into its major and minor components.
        String majorKey;
        String minorKey;
        final int colonIndex = key.indexOf(':');
        if (colonIndex >= 0) {
            majorKey = key.substring(0, colonIndex);
            minorKey = key.substring(colonIndex + 1);
        } else {
            majorKey = key;
            minorKey = "";
        }

        // Create the resolver.
        switch (majorKey) {
            case "className": return createClassNameResolver();
            case "message": return createMessageResolver(context);
            case "stackTrace": return createStackTraceResolver(context, minorKey);
        }
        throw new IllegalArgumentException("unknown key: " + key);

    }

    abstract EventResolver createClassNameResolver();

    abstract EventResolver createMessageResolver(final EventResolverContext context);

    private EventResolver createStackTraceResolver(
            final EventResolverContext context,
            final String minorKey) {
        if (!context.isStackTraceEnabled()) {
            return NULL_RESOLVER;
        }
        switch (minorKey) {
            case "text": return createStackTraceTextResolver(context);
            case "": return createStackTraceObjectResolver(context);
        }
        throw new IllegalArgumentException("unknown minor key: " + minorKey);
    }

    abstract EventResolver createStackTraceTextResolver(EventResolverContext context);

    abstract EventResolver createStackTraceObjectResolver(EventResolverContext context);

}
