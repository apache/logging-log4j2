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
package org.apache.logging.log4j.perf.util;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Facilitates creating a Call Stack for testing the performance of walkign it.
 */
public class StackDriver {
    public StackTraceElement deepCall(
            final int initialDepth, final Integer targetDepth, final Function<String, StackTraceElement> supplier) {
        int depth = initialDepth;
        if (--depth == 0) {
            final Processor processor = new Processor();
            return processor.apply(targetDepth, supplier);
        }
        return deepCall(depth, targetDepth, supplier);
    }

    public static class Processor
            implements BiFunction<Integer, Function<String, StackTraceElement>, StackTraceElement> {
        private static final String FQCN = Processor.class.getName();

        @Override
        public StackTraceElement apply(final Integer initialDepth, final Function<String, StackTraceElement> function) {
            int depth = initialDepth;
            if (--depth == 0) {
                return function.apply(FQCN);
            }
            return apply(depth, function);
        }
    }
}
