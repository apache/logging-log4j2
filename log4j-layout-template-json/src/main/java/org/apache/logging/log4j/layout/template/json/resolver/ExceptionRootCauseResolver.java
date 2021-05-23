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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;

/**
 * Exception root cause resolver.
 * <p>
 * Note that this resolver is toggled by {@link
 * JsonTemplateLayout.Builder#setStackTraceEnabled(boolean) stackTraceEnabled}
 * layout configuration, which is by default populated from <tt>log4j.layout.jsonTemplate.stackTraceEnabled</tt>
 * system property.
 *
 * @see ExceptionResolver
 */
public final class ExceptionRootCauseResolver extends ExceptionResolver {

    ExceptionRootCauseResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        super(context, config);
    }

    static String getName() {
        return "exceptionRootCause";
    }

    @Override
    Throwable extractThrowable(final LogEvent logEvent) {
        final Throwable thrown = logEvent.getThrown();
        return thrown != null ? Throwables.getRootCause(thrown) : null;
    }

}
