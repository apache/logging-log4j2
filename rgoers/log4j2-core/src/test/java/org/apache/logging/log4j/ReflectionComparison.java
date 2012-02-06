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
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.Reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the cost of invoking Reflection.getCallerClass via reflection vs calling it directly.
 */
public class ReflectionComparison {

    private static Method getCallerClass;

    private static final int COUNT = 1000000;

    @BeforeClass
    public static void setupCallerCheck() {
        try {
            ClassLoader loader = Loader.getClassLoader();
            Class clazz = loader.loadClass("sun.reflect.Reflection");
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                int modifier = method.getModifiers();
                if (method.getName().equals("getCallerClass") && Modifier.isStatic(modifier)) {
                    getCallerClass = method;
                    break;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            throw  new RuntimeException(cnfe);
        }
    }

    @Test
    public void test1() {
        Timer timer = new Timer("Reflection", COUNT);
        timer.start();
        Object[] arr = new Object[1];
        arr[0] = 3;
        for (int i= 0; i < COUNT; ++i) {
            getCallerClass(arr);
        }
        timer.stop();
        System.out.println(timer.toString());
    }


    @Test
    public void test2() {
        Timer timer = new Timer("Reflection", COUNT);
        timer.start();
        for (int i= 0; i < COUNT; ++i) {

            Reflection.getCallerClass(3);
        }
        timer.stop();
        System.out.println(timer.toString());
    }

    private Class getCallerClass(Object[] array) {
        if (getCallerClass != null) {
            try {
                /*Object[] params = new Object[]{index}; */
                return (Class) getCallerClass.invoke(null, array);
            } catch (Exception ex) {
                fail(ex.getMessage());
                // logger.debug("Unable to determine caller class via Sun Reflection", ex);
            }
        }
        return null;
    }

}
