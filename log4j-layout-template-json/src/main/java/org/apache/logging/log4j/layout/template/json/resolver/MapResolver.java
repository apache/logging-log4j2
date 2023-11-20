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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

/**
 * {@link MapMessage} resolver.
 *
 * @see ReadOnlyStringMapResolver
 */
public final class MapResolver extends ReadOnlyStringMapResolver {

    MapResolver(final EventResolverContext context, final TemplateResolverConfig config) {
        super(context, config, MapResolver::toMap);
    }

    private static ReadOnlyStringMap toMap(final LogEvent logEvent) {
        final Message message = logEvent.getMessage();
        if (!(message instanceof MapMessage)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final MapMessage<?, Object> mapMessage = (MapMessage<?, Object>) message;
        return mapMessage.getIndexedReadOnlyStringMap();
    }

    static String getName() {
        return "map";
    }
}
