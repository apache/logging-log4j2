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
public class MessageFormatsPerfTest {

    private static final int LOOP_CNT = 500;
    String[] array = new String[LOOP_CNT];
    private static long stringTime = 0;
    private static long paramTime = 0;
    private static long msgFormatTime = 0;

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
        if (msgFormatTime > paramTime) {
            System.out.println(String.format("Parameterized is %1$.2f times faster than MessageFormat.",
                ((float) msgFormatTime / paramTime)));
        } else if (msgFormatTime > paramTime) {
            System.out.println(String.format("Parameterized is %1$.2f times slower than MessageFormat.",
                ((float) paramTime / msgFormatTime)));
        } else {
            System.out.println("The speed of Parameterized and MessageFormat are the same");
        }
    }

    @Test
    public void testStringPerf() {
        String testMsg = "Test message %1s %2s";
        Timer timer = new Timer("StringFormat", LOOP_CNT);
        timer.start();
        for (int i = 0; i < LOOP_CNT; ++i) {
            StringFormattedMessage msg = new StringFormattedMessage(testMsg, "Apache", "Log4j");
            array[i] = msg.getFormattedMessage();
        }
        timer.stop();
        stringTime = timer.getElapsedNanoTime();
        System.out.println(timer.toString());
    }

    @Test
    public void testMessageFormatPerf() {
        String testMsg = "Test message {0} {1}";
        Timer timer = new Timer("MessageFormat", LOOP_CNT);
        timer.start();
        for (int i = 0; i < LOOP_CNT; ++i) {
            MessageFormatMessage msg = new MessageFormatMessage(testMsg, "Apache", "Log4j");
            array[i] = msg.getFormattedMessage();
        }
        timer.stop();
        msgFormatTime = timer.getElapsedNanoTime();
        System.out.println(timer.toString());
    }

    @Test
    public void testParameterizedPerf() {
        String testMsg = "Test message {} {}";
        Timer timer = new Timer("Parameterized", LOOP_CNT);
        timer.start();
        for (int i = 0; i < LOOP_CNT; ++i) {
            ParameterizedMessage msg = new ParameterizedMessage(testMsg, "Apache", "Log4j");
            array[i] = msg.getFormattedMessage();
        }
        timer.stop();
        paramTime = timer.getElapsedNanoTime();
        System.out.println(timer.toString());
    }
}
