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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.test.appender.FailOnceAppender;
import org.junit.jupiter.api.Test;

/**
 * Tests the AppenderControlArraySet class..
 */
public class AppenderControlArraySetTest {

    @Test
    public void testInitiallyEmpty() throws Exception {
        assertThat(new AppenderControlArraySet().isEmpty()).isTrue();
        assertThat(new AppenderControlArraySet().get().length).isEqualTo(0);
    }

    private AppenderControl createControl(final String name) {
        final Appender appender = FailOnceAppender.createAppender(name, null);
        return new AppenderControl(appender, Level.INFO, null);
    }

    @Test
    public void testAddMakesNonEmpty() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertThat(set.isEmpty()).isTrue();
        set.add(createControl("A"));
        assertThat(set.isEmpty()).isFalse();
    }

    @Test
    public void testAddReturnsTrueIfSuccessfullyAdded() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertThat(set.add(createControl("A"))).isTrue();
        assertThat(set.add(createControl("B"))).isTrue();
        assertThat(set.add(createControl("C"))).isTrue();
    }

    @Test
    public void testAddDoesNotAppendersWithSameName() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("B"), createControl("B"), createControl("A")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertThat(set.get().length).isEqualTo(2);
        assertThat(set.get()[0]).isSameAs(controls[0]);
        assertThat(set.get()[1]).isSameAs(controls[1]);
    }

    @Test
    public void testAddReturnsFalseIfAlreadyInSet() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertThat(set.add(createControl("A"))).isTrue();
        assertThat(set.add(createControl("B"))).isTrue();
        assertThat(set.add(createControl("B"))).isFalse();
        assertThat(set.add(createControl("B"))).isFalse();
        assertThat(set.add(createControl("A"))).isFalse();
        assertThat(set.get().length).isEqualTo(2);
    }

    @Test
    public void testRemoveRemovesItemFromSet() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        set.add(createControl("A"));
        set.add(createControl("B"));
        set.add(createControl("C"));
        set.add(createControl("D"));
        assertThat(set.get().length).isEqualTo(4);

        set.remove("B");
        assertThat(set.get().length).isEqualTo(3);
        final AppenderControl[] three = set.get();
        assertThat(three[0].getAppenderName()).isEqualTo("A");
        assertThat(three[1].getAppenderName()).isEqualTo("C");
        assertThat(three[2].getAppenderName()).isEqualTo("D");

        set.remove("C");
        assertThat(set.get().length).isEqualTo(2);
        final AppenderControl[] two = set.get();
        assertThat(two[0].getAppenderName()).isEqualTo("A");
        assertThat(two[1].getAppenderName()).isEqualTo("D");

        set.remove("A");
        assertThat(set.get().length).isEqualTo(1);
        final AppenderControl[] one = set.get();
        assertThat(one[0].getAppenderName()).isEqualTo("D");

        set.remove("D");
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    public void testRemoveReturnsRemovedItem() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("C"), createControl("D")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertThat(set.get().length).isEqualTo(controls.length);

        final AppenderControl b = set.remove("B");
        assertThat(b).isSameAs(controls[1]);

        final AppenderControl c = set.remove("C");
        assertThat(c).isSameAs(controls[2]);
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
        assertThat(set.asMap()).isEqualTo(expected);
    }

    @Test
    public void testClearRemovesAllItems() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        set.add(createControl("A"));
        set.add(createControl("B"));
        set.add(createControl("C"));
        assertThat(set.isEmpty()).isFalse();

        set.clear();
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    public void testClearReturnsAllItems() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("C")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertThat(set.get().length).isEqualTo(3);
        final AppenderControl[] previous = set.clear();
        assertThat(controls).isEqualTo(previous);
    }

    @Test
    public void testIsEmptyMeansZeroLengthArray() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        assertThat(set.isEmpty()).isTrue();
        assertThat(set.get().length).isEqualTo(0);
    }

    @Test
    public void testGetReturnsAddedItems() throws Exception {
        final AppenderControlArraySet set = new AppenderControlArraySet();
        final AppenderControl[] controls = new AppenderControl[] {createControl("A"), createControl("B"),
                createControl("C")};
        for (final AppenderControl ctl : controls) {
            set.add(ctl);
        }
        assertThat(set.get().length).isEqualTo(3);
        assertThat(set.get()).isEqualTo(controls);
    }
}
