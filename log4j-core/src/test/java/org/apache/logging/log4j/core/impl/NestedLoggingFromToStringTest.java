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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * There are two logger.info() calls here.
 *
 * The outer one (in main()) indirectly invokes the inner one in the Thing.toString() method.
 *
 * The inner one comes out cleanly, but leaves ReusableParameterizedMessage.indices altered and this messes up
 * the output of the outer call (which is still on the stack).
 *
 * <pre>
 *     16:35:34.781 INFO  [main] problem.demo.apache.log4j2.Log4j2ProblemDemo - getX: values x=3 y=4 z=5
 *     16:35:34.782 INFO  [main] problem.demo.apache.log4j2.Log4j2ProblemDemo - getX: values x=3 y=4 z=5[Thing x=3 y=4 z=5]
 * </pre>
 * @author lwest
 * @since 2016-09-14 in recursion
 */
public class NestedLoggingFromToStringTest {

    @Rule
    public LoggerContextRule context = new LoggerContextRule("log4j-sync-to-list.xml");
    private ListAppender listAppender;
    private Logger logger;

    @Before
    public void before() {
        listAppender = context.getListAppender("List");
        logger = LogManager.getLogger(NestedLoggingFromToStringTest.class);
    }

    static class ParameterizedLoggingThing {
        final Logger innerLogger = LogManager.getLogger(ParameterizedLoggingThing.class);
        private final int x = 3, y = 4, z = 5;
        public int getX() {
            innerLogger.debug("getX: values x={} y={} z={}", x, y, z);
            return x;
        }
        @Override public String toString() {
            return "[" + this.getClass().getSimpleName() + " x=" + getX() + " y=" + y  + " z=" + z + "]";
        }
    }

    static class ObjectLoggingThing1 {
        final Logger innerLogger = LogManager.getLogger(ObjectLoggingThing1.class);
        public int getX() {
            innerLogger.trace(new ObjectLoggingThing2());
            return 999;
        }
        @Override public String toString() {
            return "[" + this.getClass().getSimpleName() + " y=" + getX() + "]";
        }
    }

    static class ObjectLoggingThing2 {
        final Logger innerLogger = LogManager.getLogger(ObjectLoggingThing2.class);
        public int getX() {
            innerLogger.trace(new ParameterizedLoggingThing());
            return 123;
        }
        @Override public String toString() {
            return "[" + this.getClass().getSimpleName() + " x=" + getX() + "]";
        }
    }

    @Test
    public void testNestedLoggingInLastArgument() {
        final ParameterizedLoggingThing it = new ParameterizedLoggingThing();
        logger.info("main: argCount={} it={}", "2", it);
        final List<String> list = listAppender.getMessages();

        final String expect1 = "DEBUG org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest$ParameterizedLoggingThing getX: values x=3 y=4 z=5";
        final String expect2 = "INFO org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest main: argCount=2 it=[ParameterizedLoggingThing x=3 y=4 z=5]";
        assertEquals(expect1, list.get(0));
        assertEquals(expect2, list.get(1));
    }

    @Test
    public void testNestedLoggingInFirstArgument() {
        final ParameterizedLoggingThing it = new ParameterizedLoggingThing();
        logger.info("next: it={} some{} other{}", it, "AA", "BB");
        final List<String> list = listAppender.getMessages();

        final String expect1 = "DEBUG org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest$ParameterizedLoggingThing getX: values x=3 y=4 z=5";
        final String expect2 = "INFO org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest next: it=[ParameterizedLoggingThing x=3 y=4 z=5] someAA otherBB";
        assertEquals(expect1, list.get(0));
        assertEquals(expect2, list.get(1));
    }

    @Test
    public void testDoublyNestedLogging() {
        logger.info(new ObjectLoggingThing1());
        final List<String> list = listAppender.getMessages();

        final String expect1 = "DEBUG org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest$ParameterizedLoggingThing getX: values x=3 y=4 z=5";
        final String expect2 = "TRACE org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest$ObjectLoggingThing2 [ParameterizedLoggingThing x=3 y=4 z=5]";
        final String expect3 = "TRACE org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest$ObjectLoggingThing1 [ObjectLoggingThing2 x=123]";
        final String expect4 = "INFO org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest [ObjectLoggingThing1 y=999]";
        assertEquals(expect1, list.get(0));
        assertEquals(expect2, list.get(1));
        assertEquals(expect3, list.get(2));
        assertEquals(expect4, list.get(3));
    }

}