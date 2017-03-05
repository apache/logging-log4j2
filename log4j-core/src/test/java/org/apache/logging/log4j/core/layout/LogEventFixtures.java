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
package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;

import static org.junit.Assert.*;

class LogEventFixtures {

    /**
     * @return a log event that uses all the bells and whistles, features, nooks and crannies
     */
    static Log4jLogEvent createLogEvent() {
        final Marker cMarker = MarkerManager.getMarker("Marker1");
        final Marker pMarker1 = MarkerManager.getMarker("ParentMarker1");
        final Marker pMarker2 = MarkerManager.getMarker("ParentMarker2");
        final Marker gfMarker = MarkerManager.getMarker("GrandFatherMarker");
        final Marker gmMarker = MarkerManager.getMarker("GrandMotherMarker");
        cMarker.addParents(pMarker1);
        cMarker.addParents(pMarker2);
        pMarker1.addParents(gmMarker);
        pMarker1.addParents(gfMarker);
        final Exception sourceHelper = new Exception();
        sourceHelper.fillInStackTrace();
        final Exception cause = new NullPointerException("testNPEx");
        sourceHelper.fillInStackTrace();
        final StackTraceElement source = sourceHelper.getStackTrace()[0];
        final IOException ioException = new IOException("testIOEx", cause);
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 1"));
        ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 2"));
        final ThrowableProxy throwableProxy = new ThrowableProxy(ioException);
        final Map<String, String> contextMap = new HashMap<>();
        contextMap.put("MDC.A", "A_Value");
        contextMap.put("MDC.B", "B_Value");
        final DefaultThreadContextStack contextStack = new DefaultThreadContextStack(true);
        contextStack.clear();
        contextStack.push("stack_msg1");
        contextStack.add("stack_msg2");
        final Log4jLogEvent expected = Log4jLogEvent.newBuilder() //
                .setLoggerName("a.B") //
                .setMarker(cMarker) //
                .setLoggerFqcn("f.q.c.n") //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Msg")) //
                .setThrown(ioException) //
                .setThrownProxy(throwableProxy) //
                .setContextMap(contextMap) //
                .setContextStack(contextStack) //
                .setThreadName("MyThreadName") //
                .setSource(source) //
                .setTimeMillis(1).build();
        // validate event?
        return expected;
    }

    @SuppressWarnings("deprecation")
    static void assertEqualLogEvents(final LogEvent expected, final LogEvent actual, final boolean includeSource,
            final boolean includeContext, final boolean includeStacktrace) {
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(includeContext ? expected.getContextData() : ContextDataFactory.createContextData(), actual.getContextData());
        assertEquals(includeContext ? expected.getContextMap() : Collections.EMPTY_MAP, actual.getContextMap());
        assertEquals(expected.getContextStack(), actual.getContextStack());
        assertEquals(expected.getLevel(), actual.getLevel());
        assertEquals(expected.getLoggerName(), actual.getLoggerName());
        assertEquals(expected.getLoggerFqcn(), actual.getLoggerFqcn());
        assertEquals(expected.getMarker(), actual.getMarker());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getTimeMillis(), actual.getTimeMillis());
        assertEquals(includeSource ? expected.getSource() : null, actual.getSource());
        assertEquals(expected.getThreadName(), actual.getThreadName());
        assertNotNull("original should have an exception", expected.getThrown());
        assertNull("exception should not be serialized", actual.getThrown());
        if (includeStacktrace) { // TODO should compare the rest of the ThrowableProxy
            assertEquals(expected.getThrownProxy(), actual.getThrownProxy());
        }
        assertEquals(expected.isEndOfBatch(), actual.isEndOfBatch());
        assertEquals(expected.isIncludeLocation(), actual.isIncludeLocation());

        // original: non-null thrown & null thrownProxy
        // deserialized: null thrown & non-null thrownProxy
        assertNotEquals(expected.hashCode(), actual.hashCode());
        assertNotEquals(expected, actual);
    }

}
