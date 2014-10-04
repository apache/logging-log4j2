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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import sun.reflect.Reflection;

/**
 * <p>
 * Benchmarks the different ways the caller class can be obtained. To run this in sampling mode (latency test):
 * </p>
 *
 * <pre>
 *     java -jar benchmarks.jar ".*ReflectionBenchmark.*" -i 5 -f 1 -wi 5 -bm sample -tu ns
 * </pre>
 * <p>
 * To run this in throughput testing mode:
 * </p>
 *
 * <pre>
 *     java -jar benchmarks.jar ".*ReflectionBenchmark.*" -i 5 -f 1 -wi 5 -bm Throughput -tu ms
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

    @State(Scope.Benchmark)
    public static class ClassContextManager extends SecurityManager {
        @Override
        protected Class[] getClassContext() {
            return super.getClassContext();
        }
    }

    @Benchmark
    public void baseline() {
    }

    @Benchmark
    public String test01_getCallerClassNameFromStackTrace() {
        return new Throwable().getStackTrace()[3].getClassName();
    }

    @Benchmark
    public String test02_getCallerClassNameFromThreadStackTrace() {
        return Thread.currentThread().getStackTrace()[3].getClassName();
    }

    @Benchmark
    public String test03_getCallerClassNameReflectively() {
        return ReflectionUtil.getCallerClass(3).getName();
    }

    @Benchmark
    public String test04_getCallerClassNameSunReflection() {
        return Reflection.getCallerClass(3).getName();
    }

    @Benchmark
    public Class<?> test05_getStackTraceClassForClassName() throws ClassNotFoundException {
        return Class.forName(new Throwable().getStackTrace()[3].getClassName());
    }

    @Benchmark
    public Class<?> test06_getThreadStackTraceClassForClassName() throws ClassNotFoundException {
        return Class.forName(Thread.currentThread().getStackTrace()[3].getClassName());
    }

    @Benchmark
    public Class<?> test07_getReflectiveCallerClassUtility() {
        return ReflectionUtil.getCallerClass(3);
    }

    @Benchmark
    public Class<?> test08_getDirectSunReflection() {
        return Reflection.getCallerClass(3);
    }

    @Benchmark
    public Message test09_getMessageUsingNew(final RandomInteger rng) {
        return new StringFormattedMessage("Hello %i", rng.random);
    }

    @Benchmark
    public Message test10_getMessageUsingReflection(final RandomInteger rng) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        final Constructor<? extends Message> constructor = StringFormattedMessage.class.getConstructor(String.class,
                Object[].class);
        return constructor.newInstance("Hello %i", new Object[] { rng.random });
    }

    @Benchmark
    public Class<?>[] test11_getClassContextViaCallerClass() {
        // let's not benchmark LinkedList or anything here
        final Class<?>[] classes = new Class<?>[100];
        Class<?> clazz;
        for (int i = 0; null != (clazz = ReflectionUtil.getCallerClass(i)); i++) {
            classes[i] = clazz;
        }
        return classes;
    }

    @Benchmark
    public Class<?>[] test12_getClassContextViaSecurityManager(final ClassContextManager classContextManager) {
        return classContextManager.getClassContext();
    }

}
