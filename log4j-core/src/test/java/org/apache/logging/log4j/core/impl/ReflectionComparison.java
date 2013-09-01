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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.core.Timer;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.junit.Test;

import sun.reflect.Reflection;

import java.lang.reflect.Constructor;

import static org.junit.Assert.assertEquals;

/**
 * Tests the cost of invoking Reflection.getCallerClass via reflection vs calling it directly.
 */
public class ReflectionComparison {

    private static final int COUNT = 1000000;

    @Test
    public void testReflection() {
        final Timer timer = new Timer("Reflection", COUNT);
        timer.start();
        for (int i= 0; i < COUNT; ++i) {
            ReflectiveCallerClassUtility.getCaller(3);
        }
        timer.stop();
        System.out.println(timer.toString());
    }

    @Test
    public void testDirectly() {
        final Timer timer = new Timer("Directly", COUNT);
        timer.start();
        for (int i= 0; i < COUNT; ++i) {

            Reflection.getCallerClass(3);
        }
        timer.stop();
        System.out.println(timer.toString());
    }

    @Test
    public void testBothMethodsReturnTheSame() {
        assertEquals("1 is not the same.",
                Reflection.getCallerClass(1 + ReflectiveCallerClassUtility.JAVA_7U25_COMPENSATION_OFFSET),
                ReflectiveCallerClassUtility.getCaller(1));
        assertEquals("2 is not the same.",
                Reflection.getCallerClass(2 + ReflectiveCallerClassUtility.JAVA_7U25_COMPENSATION_OFFSET),
                ReflectiveCallerClassUtility.getCaller(2));
        assertEquals("3 is not the same.",
                Reflection.getCallerClass(3 + ReflectiveCallerClassUtility.JAVA_7U25_COMPENSATION_OFFSET),
                ReflectiveCallerClassUtility.getCaller(3));
        assertEquals("4 is not the same.",
                Reflection.getCallerClass(4 + ReflectiveCallerClassUtility.JAVA_7U25_COMPENSATION_OFFSET),
                ReflectiveCallerClassUtility.getCaller(4));
        assertEquals("5 is not the same.",
                Reflection.getCallerClass(5 + ReflectiveCallerClassUtility.JAVA_7U25_COMPENSATION_OFFSET),
                ReflectiveCallerClassUtility.getCaller(5));
        assertEquals("6 is not the same.",
                Reflection.getCallerClass(6 + ReflectiveCallerClassUtility.JAVA_7U25_COMPENSATION_OFFSET),
                ReflectiveCallerClassUtility.getCaller(6));
    }

    @Test
    public void testCreateObjects() throws Exception {
        Timer timer = new Timer("CreatObjects", COUNT);
        timer.start();
        for (int i = 0; i < COUNT; ++i) {
            new StringFormattedMessage("Hello %1", i);
        }
        timer.stop();
        System.out.println(timer.toString());
        final Class<? extends Message> clazz = StringFormattedMessage.class;

        timer = new Timer("ReflectionObject", COUNT);
        timer.start();

        for (int i = 0; i < COUNT; ++i) {
            createMessage(clazz, "Hello %1", i);
        }
        timer.stop();
        System.out.println(timer.toString());
    }

    private Message createMessage(final Class<? extends Message> clazz, final String msg, final Object... params) throws Exception {
        final Constructor<? extends Message> constructor = clazz.getConstructor(String.class, Object[].class);
        return constructor.newInstance(msg, params);
    }

}
