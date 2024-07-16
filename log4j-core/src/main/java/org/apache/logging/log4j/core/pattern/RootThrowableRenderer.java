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
package org.apache.logging.log4j.core.pattern;

import java.util.List;

import org.apache.logging.log4j.core.util.internal.StringBuilders;

class RootThrowableRenderer extends ThrowableRenderer<ThrowableRenderer.Context> {

    static final String WRAPPED_BY_LABEL = "Wrapped by: ";

    RootThrowableRenderer(List<String> ignoredPackageNames, String lineSeparator, int maxLineCount) {
        super(ignoredPackageNames, lineSeparator, maxLineCount);
    }

    @Override
    void format(
            final StringBuilder buffer,
            final Throwable throwable,
            final Context context,
            final String stackTraceElementSuffix) {
        final Throwable cause = throwable.getCause();
        if (cause != null) {
            format(buffer, throwable.getCause(), context, stackTraceElementSuffix);
            buffer.append(WRAPPED_BY_LABEL);
            appendSuffix(buffer, stackTraceElementSuffix);
        }
        renderThrowableMessage(buffer, throwable);
        appendSuffix(buffer, stackTraceElementSuffix);
        appendLineSeparator(buffer, lineSeparator);
        formatElements(buffer, throwable, context, stackTraceElementSuffix);
    }
}
