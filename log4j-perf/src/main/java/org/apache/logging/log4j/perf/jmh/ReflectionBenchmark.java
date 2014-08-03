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
package org.apache.logging.log4j.perf.jmh;

import org.apache.logging.log4j.core.impl.ReflectiveCallerClassUtility;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;
import sun.reflect.Reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

/**
 * <p>Benchmarks the different ways the caller class can be obtained.
 * To run this in sampling mode (latency test):</p>
 * <pre>
 *     java -jar microbenchmarks.jar ".*ReflectionBenchmark.*" -i 5 -f 1 -wi 5 -bm sample -tu ns
 * </pre>
 * <p>To run this in throughput testing mode:</p>
 * <pre>
 *     java -jar microbenchmarks.jar ".*ReflectionBenchmark.*" -i 5 -f 1 -wi 5 -bm Throughput -tu ms
 * </pre>
 */
public class ReflectionBenchmark {

    @State(Scope.Thread)
    public static class RandomInteger {

        private final Random r = new Random();

        int random;

        @Setup(Level.Iteration)
        public void setup() {
            random = r.nextInt();
        }
    }

    @GenerateMicroBenchmark
    public void testBaseline(final BlackHole bh) {
    }

    @GenerateMicroBenchmark
    public String getCallerClassNameFromStackTrace(final BlackHole bh) {
        return new Throwable().getStackTrace()[3].getClassName();
    }

    @GenerateMicroBenchmark
    public String getCallerClassNameReflectively(final BlackHole bh) {
        return ReflectiveCallerClassUtility.getCaller(3).getName();
    }

    @GenerateMicroBenchmark
    public String getCallerClassNameSunReflection(final BlackHole bh) {
        return Reflection.getCallerClass(3).getName();
    }

    @GenerateMicroBenchmark
    public Class<?> getStackTraceClassForClassName() throws ClassNotFoundException {
        return Class.forName(new Throwable().getStackTrace()[3].getClassName());
    }

    @GenerateMicroBenchmark
    public Class<?> getReflectiveCallerClassUtility(final BlackHole bh) {
        return ReflectiveCallerClassUtility.getCaller(3);
    }

    @GenerateMicroBenchmark
    public Class<?> getDirectSunReflection(final BlackHole bh) {
        return Reflection.getCallerClass(3);
    }

    @GenerateMicroBenchmark
    public Message getMessageUsingNew(final RandomInteger rng) {
        return new StringFormattedMessage("Hello %i", rng.random);
    }

    @GenerateMicroBenchmark
    public Message getMessageUsingReflection(final RandomInteger rng)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Constructor<? extends Message> constructor = StringFormattedMessage.class.getConstructor(String.class,
            Object[].class);
        return constructor.newInstance("Hello %i", new Object[]{rng.random});
    }
}
