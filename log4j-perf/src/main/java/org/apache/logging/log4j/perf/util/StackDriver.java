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
package org.apache.logging.log4j.perf.util;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Facilitates creating a Call Stack for testing the performance of walkign it.
 */
public class StackDriver {
    public StackTraceElement deepCall(int initialDepth, Integer targetDepth, Function<String, StackTraceElement> supplier) {
        if (--initialDepth == 0) {
            Processor processor = new Processor();
            return processor.apply(targetDepth, supplier);
        }
        return deepCall(initialDepth, targetDepth, supplier);
    }

    public static class Processor implements BiFunction<Integer, Function<String, StackTraceElement>, StackTraceElement> {
        private static final String FQCN = Processor.class.getName();

        @Override
        public StackTraceElement apply(Integer depth, Function<String, StackTraceElement> function) {
            if (--depth == 0) {
                return function.apply(FQCN);
            }
            return apply(depth, function);
        }
    }
}
