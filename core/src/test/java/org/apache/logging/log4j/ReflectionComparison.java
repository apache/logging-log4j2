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
package org.apache.logging.log4j;

import org.apache.logging.log4j.core.Timer;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.Reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.fail;

/**
 * Tests the cost of invoking Reflection.getCallerClass via reflection vs calling it directly.
 */
public class ReflectionComparison {

    private static Method getCallerClass;

    private static final int COUNT = 1000000;

    private static Class[] paramTypes = new Class[] {String.class, Object[].class};

    @BeforeClass
    public static void setupCallerCheck() {
        try {
            final ClassLoader loader = Loader.getClassLoader();
            final Class clazz = loader.loadClass("sun.reflect.Reflection");
            final Method[] methods = clazz.getMethods();
            for (final Method method : methods) {
                final int modifier = method.getModifiers();
                if (method.getName().equals("getCallerClass") && Modifier.isStatic(modifier)) {
                    getCallerClass = method;
                    break;
                }
            }
        } catch (final ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            throw  new RuntimeException(cnfe);
        }
    }

    @Test
    public void test1() {
        final Timer timer = new Timer("Reflection", COUNT);
        timer.start();
        final Object[] arr = new Object[1];
        arr[0] = 3;
        for (int i= 0; i < COUNT; ++i) {
            getCallerClass(arr);
        }
        timer.stop();
        System.out.println(timer.toString());
    }


    @Test
    public void test2() {
        final Timer timer = new Timer("Reflection", COUNT);
        timer.start();
        for (int i= 0; i < COUNT; ++i) {

            Reflection.getCallerClass(3);
        }
        timer.stop();
        System.out.println(timer.toString());
    }


    @Test
    public void createObjects() throws Exception {
        Timer timer = new Timer("NewObject", COUNT);
        timer.start();
        Message msg;
        for (int i = 0; i < COUNT; ++i) {
            msg = new StringFormattedMessage("Hello %1", i);
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

    private Class getCallerClass(final Object[] array) {
        if (getCallerClass != null) {
            try {
                /*Object[] params = new Object[]{index}; */
                return (Class) getCallerClass.invoke(null, array);
            } catch (final Exception ex) {
                fail(ex.getMessage());
                // logger.debug("Unable to determine caller class via Sun Reflection", ex);
            }
        }
        return null;
    }

    private Message createMessage(final Class<? extends Message> clazz, final String msg, final Object... params) throws Exception {
        final Constructor<? extends Message> constructor = clazz.getConstructor(paramTypes);
        return constructor.newInstance(msg, params);
    }

}
