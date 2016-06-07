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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.test.appender.FailOnceAppender;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests the AppenderControlArraySet class..
 */
public class AppenderControlArraySetTest {

    @Test
    public void testInitiallyEmpty() throws Exception {
        assertTrue(new AppenderControlArraySet().isEmpty());
        assertTrue(new AppenderControlArraySet().get().length == 0);
    }

    private AppenderControl createControl(final String name) {
        final Appender appender = FailOnceAppender.createAppender(name);
        return new AppenderControl(appender, Level.INFO, null);
    }

    @Test
    public void testAddMakesNonEmpty() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertTrue(set.isEmpty());
        set.add(createControl("A"));
        assertFalse(set.isEmpty());
    }

    @Test
    public void testAddReturnsTrueIfSuccessfullyAdded() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertTrue(set.add(createControl("A")));
        assertTrue(set.add(createControl("B")));
        assertTrue(set.add(createControl("C")));
    }

    @Test
    public void testAddDoesNotAppendersWithSameName() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("B"), createControl("B"), createControl("A")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertEquals(2, set.get().length);
        assertSame(controls[0], set.get()[0]);
        assertSame(controls[1], set.get()[1]);
    }

    @Test
    public void testAddReturnsFalseIfAlreadyInSet() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertTrue(set.add(createControl("A")));
        assertTrue(set.add(createControl("B")));
        assertFalse(set.add(createControl("B")));
        assertFalse(set.add(createControl("B")));
        assertFalse(set.add(createControl("A")));
        assertEquals(2, set.get().length);
    }

    @Test
    public void testRemoveRemovesItemFromSet() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        set.add(createControl("A"));
        set.add(createControl("B"));
        set.add(createControl("C"));
        set.add(createControl("D"));
        assertEquals(4, set.get().length);

        set.remove("B");
        assertEquals(3, set.get().length);
        final AppenderControl[] three = set.get();
        assertEquals("A", three[0].getAppenderName());
        assertEquals("C", three[1].getAppenderName());
        assertEquals("D", three[2].getAppenderName());

        set.remove("C");
        assertEquals(2, set.get().length);
        final AppenderControl[] two = set.get();
        assertEquals("A", two[0].getAppenderName());
        assertEquals("D", two[1].getAppenderName());

        set.remove("A");
        assertEquals(1, set.get().length);
        final AppenderControl[] one = set.get();
        assertEquals("D", one[0].getAppenderName());

        set.remove("D");
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveReturnsRemovedItem() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("C"), createControl("D")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertEquals(controls.length, set.get().length);

        final AppenderControl b = set.remove("B");
        assertSame(controls[1], b);

        final AppenderControl c = set.remove("C");
        assertSame(controls[2], c);
    }

    @Test
    public void testAsMap() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("C"), createControl("D")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        final Map<String, Appender> expected = new HashMap<>();
        for (final AppenderControl ctl : controls) {
            expected.put(ctl.getAppenderName(), ctl.getAppender());
        }
        assertEquals(expected, set.asMap());
    }

    @Test
    public void testClearRemovesAllItems() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        set.add(createControl("A"));
        set.add(createControl("B"));
        set.add(createControl("C"));
        assertFalse(set.isEmpty());

        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testClearReturnsAllItems() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("C")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertEquals(3, set.get().length);
        final AppenderControl[] previous = set.clear();
        assertArrayEquals(previous, controls);
    }

    @Test
    public void testIsEmptyMeansZeroLengthArray() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertTrue(set.isEmpty());
        assertTrue(set.get().length == 0);
    }

    @Test
    public void testGetReturnsAddedItems() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("C")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertEquals(3, set.get().length);
        assertArrayEquals(controls, set.get());
    }
}