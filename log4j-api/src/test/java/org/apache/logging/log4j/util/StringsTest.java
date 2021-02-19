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

package org.apache.logging.log4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

public class StringsTest {

    @Test
    public void testIsEmpty() {
        assertThat(Strings.isEmpty(null)).isTrue();
        assertThat(Strings.isEmpty("")).isTrue();
        assertThat(Strings.isEmpty(" ")).isFalse();
        assertThat(Strings.isEmpty("a")).isFalse();
    }

    @Test
    public void testIsBlank() {
        assertThat(Strings.isBlank(null)).isTrue();
        assertThat(Strings.isBlank("")).isTrue();
        assertThat(Strings.isBlank(" ")).isTrue();
        assertThat(Strings.isBlank("\n")).isTrue();
        assertThat(Strings.isBlank("\r")).isTrue();
        assertThat(Strings.isBlank("\t")).isTrue();
        assertThat(Strings.isEmpty("a")).isFalse();
    }

    /**
     * A sanity test to make sure a typo does not mess up {@link Strings#EMPTY}.
     */
    @Test
    public void testEMPTY() {
        assertThat(Strings.EMPTY).isEqualTo("");
        assertThat(Strings.EMPTY.length()).isEqualTo(0);
    }

    @Test
    public void testJoin() {
        assertThat(Strings.join((Iterable<?>) null, '.')).isNull();
        assertThat(Strings.join((Iterator<?>) null, '.')).isNull();
        assertThat(Strings.join((Collections.emptyList()), '.')).isEqualTo("");

        assertThat(Strings.join(Collections.singletonList("a"), '.')).isEqualTo("a");
        assertThat(Strings.join(Arrays.asList("a", "b"), '.')).isEqualTo("a.b");
        assertThat(Strings.join(Arrays.asList("a", "b", "c"), '.')).isEqualTo("a.b.c");

        assertThat(Strings.join(Collections.singletonList((String) null), ':')).isEqualTo("");
        assertThat(Strings.join(Arrays.asList(null, null), ':')).isEqualTo(":");
        assertThat(Strings.join(Arrays.asList("a", null), ':')).isEqualTo("a:");
        assertThat(Strings.join(Arrays.asList(null, "b"), ':')).isEqualTo(":b");
    }

    @Test
    public void testQuote() {
        assertThat(Strings.quote("Q")).isEqualTo("'Q'");
    }

}
