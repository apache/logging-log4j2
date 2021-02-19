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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.apache.logging.log4j.util.Timer;

public class ThreadContextUtilityClass {

    public static void perfTest() {
        ThreadContext.clearMap();
        final Timer complete = new Timer("ThreadContextTest");
        complete.start();
        ThreadContext.put("Var1", "value 1");
        ThreadContext.put("Var2", "value 2");
        ThreadContext.put("Var3", "value 3");
        ThreadContext.put("Var4", "value 4");
        ThreadContext.put("Var5", "value 5");
        ThreadContext.put("Var6", "value 6");
        ThreadContext.put("Var7", "value 7");
        ThreadContext.put("Var8", "value 8");
        ThreadContext.put("Var9", "value 9");
        ThreadContext.put("Var10", "value 10");
        final int loopCount = 1000000;
        final Timer timer = new Timer("ThreadContextCopy", loopCount);
        timer.start();
        for (int i = 0; i < loopCount; ++i) {
            final Map<String, String> map = ThreadContext.getImmutableContext();
            assertThat(map).isNotNull();
        }
        timer.stop();
        complete.stop();
        System.out.println(timer.toString());
        System.out.println(complete.toString());
    }


    public static void testGetContextReturnsEmptyMapIfEmpty() {
        ThreadContext.clearMap();
        assertThat(ThreadContext.getContext().isEmpty()).isTrue();
    }


    public static void testGetContextReturnsMutableCopy() {
        ThreadContext.clearMap();
        final Map<String, String> map1 = ThreadContext.getContext();
        assertThat(map1.isEmpty()).isTrue();
        map1.put("K", "val"); // no error
        assertThat(map1.get("K")).isEqualTo("val");

        // adding to copy does not affect thread context map
        assertThat(ThreadContext.getContext().isEmpty()).isTrue();

        ThreadContext.put("key", "val2");
        final Map<String, String> map2 = ThreadContext.getContext();
        assertThat(map2.size()).isEqualTo(1);
        assertThat(map2.get("key")).isEqualTo("val2");
        map2.put("K", "val"); // no error
        assertThat(map2.get("K")).isEqualTo("val");

        // first copy is not affected
        assertThat(map2).isNotSameAs(map1);
        assertThat(map1.size()).isEqualTo(1);
    }

    public static void testGetImmutableContextReturnsEmptyMapIfEmpty() {
        ThreadContext.clearMap();
        assertThat(ThreadContext.getImmutableContext().isEmpty()).isTrue();
    }


    public static void testGetImmutableContextReturnsImmutableMapIfNonEmpty() {
        ThreadContext.clearMap();
        ThreadContext.put("key", "val");
        final Map<String, String> immutable = ThreadContext.getImmutableContext();
        assertThatThrownBy(() -> immutable.put("otherkey", "otherval")).isInstanceOf(UnsupportedOperationException.class);
    }

    public static void testGetImmutableContextReturnsImmutableMapIfEmpty() {
        ThreadContext.clearMap();
        final Map<String, String> immutable = ThreadContext.getImmutableContext();
        assertThatThrownBy(() -> immutable.put("otherkey", "otherval")).isInstanceOf(UnsupportedOperationException.class);
    }

    public static void testGetImmutableStackReturnsEmptyStackIfEmpty() {
        ThreadContext.clearStack();
        assertThat(ThreadContext.getImmutableStack().asList().isEmpty()).isTrue();
    }


    public static void testPut() {
        ThreadContext.clearMap();
        assertThat(ThreadContext.get("testKey")).isNull();
        ThreadContext.put("testKey", "testValue");
        assertThat(ThreadContext.get("testKey")).isEqualTo("testValue");
    }
}
