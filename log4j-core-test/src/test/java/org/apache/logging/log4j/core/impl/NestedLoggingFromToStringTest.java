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
package org.apache.logging.log4j.core.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.ReconfigurationPolicy;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

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
@LoggerContextSource(value = "impl/NestedLoggingFromToStringTest.xml", reconfigure = ReconfigurationPolicy.BEFORE_EACH)
@UsingStatusListener // Record status logger messages and dump in case of a failure
public class NestedLoggingFromToStringTest {

    static class ParameterizedLoggingThing {
        final Logger innerLogger;
        private static final int x = 3, y = 4, z = 5;

        ParameterizedLoggingThing(LoggerContext loggerContext) {
            innerLogger = loggerContext.getLogger(ParameterizedLoggingThing.class);
        }

        public int getX() {
            innerLogger.debug("getX: values x={} y={} z={}", x, y, z);
            return x;
        }

        @Override
        public String toString() {
            return "[" + this.getClass().getSimpleName() + " x=" + getX() + " y=" + y + " z=" + z + "]";
        }
    }

    static class ObjectLoggingThing1 {
        final LoggerContext loggerContext;
        final Logger innerLogger;

        ObjectLoggingThing1(LoggerContext loggerContext) {
            this.loggerContext = loggerContext;
            this.innerLogger = loggerContext.getLogger(ObjectLoggingThing1.class);
        }

        public int getX() {
            innerLogger.trace(new ObjectLoggingThing2(loggerContext));
            return 999;
        }

        @Override
        public String toString() {
            return "[" + this.getClass().getSimpleName() + " y=" + getX() + "]";
        }
    }

    static class ObjectLoggingThing2 {
        final LoggerContext loggerContext;
        final Logger innerLogger;

        ObjectLoggingThing2(LoggerContext loggerContext) {
            this.loggerContext = loggerContext;
            this.innerLogger = loggerContext.getLogger(ObjectLoggingThing2.class);
        }

        public int getX() {
            innerLogger.trace(new ParameterizedLoggingThing(loggerContext));
            return 123;
        }

        @Override
        public String toString() {
            return "[" + this.getClass().getSimpleName() + " x=" + getX() + "]";
        }
    }

    @Test
    public void testNestedLoggingInLastArgument(LoggerContext loggerContext, @Named("LIST") ListAppender listAppender) {
        final ParameterizedLoggingThing it = new ParameterizedLoggingThing(loggerContext);
        loggerContext.getLogger(NestedLoggingFromToStringTest.class).info("main: argCount={} it={}", "2", it);

        assertThat(listAppender.getMessages())
                .containsExactly(
                        "DEBUG org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest.ParameterizedLoggingThing getX: values x=3 y=4 z=5",
                        "INFO org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest main: argCount=2 it=[ParameterizedLoggingThing x=3 y=4 z=5]");
    }

    @Test
    public void testNestedLoggingInFirstArgument(
            LoggerContext loggerContext, @Named("LIST") ListAppender listAppender) {
        final ParameterizedLoggingThing it = new ParameterizedLoggingThing(loggerContext);
        loggerContext.getLogger(NestedLoggingFromToStringTest.class).info("next: it={} some{} other{}", it, "AA", "BB");

        assertThat(listAppender.getMessages())
                .containsExactly(
                        "DEBUG org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest.ParameterizedLoggingThing getX: values x=3 y=4 z=5",
                        "INFO org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest next: it=[ParameterizedLoggingThing x=3 y=4 z=5] someAA otherBB");
    }

    @Test
    public void testDoublyNestedLogging(LoggerContext loggerContext, @Named("LIST") ListAppender listAppender) {
        loggerContext.getLogger(NestedLoggingFromToStringTest.class).info(new ObjectLoggingThing1(loggerContext));

        assertThat(listAppender.getMessages())
                .containsExactly(
                        "DEBUG org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest.ParameterizedLoggingThing getX: values x=3 y=4 z=5",
                        "TRACE org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest.ObjectLoggingThing2 [ParameterizedLoggingThing x=3 y=4 z=5]",
                        "TRACE org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest.ObjectLoggingThing1 [ObjectLoggingThing2 x=123]",
                        "INFO org.apache.logging.log4j.core.impl.NestedLoggingFromToStringTest [ObjectLoggingThing1 y=999]");
    }
}
