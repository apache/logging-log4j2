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
package org.apache.log4j;

import org.apache.log4j.helpers.QuietWriter;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;

/**
 * Used to test Log4j 1 support. All we are looking for here is that this code compiles.
 */
public class CustomConsoleAppender extends ConsoleAppender {

    public CustomConsoleAppender() {
        super();
    }

    public CustomConsoleAppender(final Layout layout) {
        super(layout);
    }

    public CustomConsoleAppender(final Layout layout, final String target) {
        super(layout, target);
    }

    @SuppressWarnings({"cast", "unused"})
    public void compilerAccessToConsoleAppenderInstanceVariables() {
        if (target instanceof String) {
            final String other = name;
        }
    }

    @SuppressWarnings({"cast", "unused"})
    public void compilerAccessToWriterAppenderInstanceVariables() {
        if (immediateFlush) {
            final boolean other = immediateFlush;
        }
        if (encoding instanceof String) {
            final String other = encoding;
        }
        if (qw instanceof QuietWriter) {
            final QuietWriter other = qw;
        }
    }

    @SuppressWarnings({"cast", "unused"})
    public void compilerAccessToWriterAppenderSkeletonVariables() {
        if (closed) {
            final boolean compileMe = closed;
        }
        if (errorHandler instanceof ErrorHandler) {
            final ErrorHandler other = errorHandler;
        }
        if (headFilter instanceof Filter) {
            final Filter other = headFilter;
        }
        if (layout instanceof Layout) {
            final Layout other = layout;
        }
        if (name instanceof String) {
            final String other = name;
        }
        if (tailFilter instanceof Filter) {
            final Filter other = tailFilter;
        }
        if (threshold instanceof Priority) {
            final Priority other = threshold;
        }
    }
}
