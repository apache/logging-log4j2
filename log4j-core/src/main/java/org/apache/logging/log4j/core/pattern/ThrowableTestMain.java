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

public class ThrowableTestMain {

    public static void main(String[] args) {
        Throwable r = createException("r", 1, 3);
        renderException(r, null);
        renderException(r, new ThrowableRenderer<>(Collections.emptyList(), System.lineSeparator(), Integer.MAX_VALUE));
        renderException(
                r, new ExtendedThrowableRenderer(Collections.emptyList(), System.lineSeparator(), Integer.MAX_VALUE));
        renderException(
                r, new RootThrowableRenderer(Collections.emptyList(), System.lineSeparator(), Integer.MAX_VALUE));
    }

    private static Throwable createException(String name, int depth, int maxDepth) {
        Exception r = new Exception(name);
        if (depth < maxDepth) {
            r.initCause(createException(name + "_c", depth + 1, maxDepth));
            r.addSuppressed(createException(name + "_s", depth + 1, maxDepth));
        }
        return r;
    }

    private static void renderException(Throwable throwable, ThrowableRenderer<?> renderer) {
        if (renderer == null) {
            System.out.format("%n=== %-25s ==============================%n%n", "Throwable");
            throwable.printStackTrace();
        } else {
            System.out.format(
                    "%n=== %-25s ==============================%n%n",
                    renderer.getClass().getSimpleName());
            final StringBuilder stringBuilder = new StringBuilder();
            renderer.renderThrowable(stringBuilder, throwable, "");
            System.out.println(stringBuilder);
        }
    }
}
