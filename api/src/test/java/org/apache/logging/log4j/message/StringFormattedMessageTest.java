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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.Timer;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class StringFormattedMessageTest {

    private static final int LOOP_CNT = 500;
    String[] array = new String[LOOP_CNT];
    private static long stringTime = 0;
    private static long paramTime = 0;

    @AfterClass
    public static void after() {
        if (stringTime > paramTime) {
            System.out.println(String.format("Parameterized is %1$.2f times faster than StringFormat.",
                ((float) stringTime / paramTime)));
        } else if (stringTime > paramTime) {
            System.out.println(String.format("Parameterized is %1$.2f times slower than StringFormat.",
                ((float) paramTime / stringTime)));
        } else {
            System.out.println("The speed of Parameterized and StringFormat are the same");
        }
    }

    @Test
    public void testNoArgs() {
        final String testMsg = "Test message %1s";
        StringFormattedMessage msg = new StringFormattedMessage(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        final String expected = "Test message null";
        assertEquals(expected, result);
        final Object[] array = null;
        msg = new StringFormattedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertEquals(expected, result);
    }

    @Test
    public void testOneArg() {
        final String testMsg = "Test message %1s";
        final StringFormattedMessage msg = new StringFormattedMessage(testMsg, "Apache");
        final String result = msg.getFormattedMessage();
        final String expected = "Test message Apache";
        assertEquals(expected, result);
    }

    @Test
    public void testStringPerf() {
        final String testMsg = "Test message %1s %2s";
        final Timer timer = new Timer("StringFormat", LOOP_CNT);
        timer.start();
        for (int i = 0; i < LOOP_CNT; ++i) {
            final StringFormattedMessage msg = new StringFormattedMessage(testMsg, "Apache", "Log4j");
            array[i] = msg.getFormattedMessage();
        }
        timer.stop();
        stringTime = timer.getElapsedNanoTime();
        System.out.println(timer.toString());
    }

    @Test
    public void testParameterizedPerf() {
        final String testMsg = "Test message {} {}";
        final Timer timer = new Timer("Parameterized", LOOP_CNT);
        timer.start();
        for (int i = 0; i < LOOP_CNT; ++i) {
            final ParameterizedMessage msg = new ParameterizedMessage(testMsg, "Apache", "Log4j");
            array[i] = msg.getFormattedMessage();
        }
        timer.stop();
        paramTime = timer.getElapsedNanoTime();
        System.out.println(timer.toString());
    }
}
