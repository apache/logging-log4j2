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

import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

public class ThrowableTestMain {

    public static void main(String[] args) {
        Throwable r = createException("r", 1, 3);
        renderException(r);
        renderException(r, new ThrowableRenderer<>(Collections.emptyList(), Integer.MAX_VALUE));
        renderException(r, "%ex");
        renderException(r, new ExtendedThrowableRenderer(Collections.emptyList(), Integer.MAX_VALUE));
        renderException(r, "%xEx");
        renderException(r, new RootThrowableRenderer(Collections.emptyList(), Integer.MAX_VALUE));
        renderException(r, "%rEx");
    }

    private static Throwable createException(String name, int depth, int maxDepth) {
        Exception r = new Exception(name);
        if (depth < maxDepth) {
            r.initCause(createException(name + "_c", depth + 1, maxDepth));
            r.addSuppressed(createException(name + "_s", depth + 1, maxDepth));
        }
        return r;
    }

    private static void renderException(Throwable throwable) {
        System.out.format("%n=== %-25s ==============================%n%n", "Throwable");
        throwable.printStackTrace(System.out);
    }

    private static void renderException(Throwable throwable, ThrowableRenderer<?> renderer) {
        System.out.format(
                "%n=== %-25s ==============================%n%n",
                renderer.getClass().getSimpleName());
        final StringBuilder stringBuilder = new StringBuilder();
        renderer.renderThrowable(stringBuilder, throwable, System.lineSeparator());
        System.out.println(stringBuilder);
    }

    private static void renderException(Throwable throwable, String pattern) {
        System.out.format("%n=== %-25s ==============================%n%n", String.format("pattern(\"%s\")", pattern));
        PatternParser parser = new PatternParser(PatternConverter.CATEGORY);
        List<PatternFormatter> formatters = parser.parse(pattern);
        if (formatters.size() != 1) {
            throw new IllegalArgumentException("was expecting a single formatter, found " + formatters.size());
        }
        PatternFormatter formatter = formatters.get(0);
        Log4jLogEvent logEvent = Log4jLogEvent.newBuilder().setThrown(throwable).build();
        StringBuilder stringBuilder = new StringBuilder();
        formatter.format(logEvent, stringBuilder);
        System.out.println(stringBuilder);
    }
}
