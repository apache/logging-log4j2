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
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class MarkerFilterTest {

    @Test
    public void testMarkers() {
        final Marker parent = MarkerManager.getMarker("Parent");
        final Marker child = MarkerManager.getMarker("Child").setParents(parent);
        final Marker grandChild = MarkerManager.getMarker("GrandChild").setParents(child);
        final Marker sibling = MarkerManager.getMarker("Sibling").setParents(parent);
        final Marker stranger = MarkerManager.getMarker("Stranger");
        MarkerFilter filter = MarkerFilter.createFilter("Parent", null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.DENY, filter.filter(null, null, stranger, (Object) null, (Throwable) null));
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, null, child, (Object) null, (Throwable) null));
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, null, grandChild, (Object) null, (Throwable) null));
        filter.stop();
        LogEvent event = Log4jLogEvent.newBuilder() //
                .setMarker(grandChild) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Hello, world!")).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event));
        filter = MarkerFilter.createFilter("Child", null, null);
        filter.start();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event));
        event = Log4jLogEvent.newBuilder() //
                .setMarker(sibling) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("Hello, world!")).build();
        assertSame(Filter.Result.DENY, filter.filter(event));
        filter.stop();
    }
}
