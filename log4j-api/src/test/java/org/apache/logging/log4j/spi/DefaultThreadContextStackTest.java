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
package org.apache.logging.log4j.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UsingAnyThreadContext
public class DefaultThreadContextStackTest {

    @BeforeEach
    public void before() {
        // clear the thread-local map
        new DefaultThreadContextMap(true).clear();
    }

    @Test
    public void testEqualsVsSameKind() {
        final DefaultThreadContextStack stack1 = createStack();
        final DefaultThreadContextStack stack2 = createStack();
        assertThat(stack1).isEqualTo(stack1);
        assertThat(stack2).isEqualTo(stack2);
        assertThat(stack2).isEqualTo(stack1);
        assertThat(stack1).isEqualTo(stack2);
    }

    @Test
    public void testEqualsVsMutable() {
        final ThreadContextStack stack1 = createStack();
        final ThreadContextStack stack2 = MutableThreadContextStackTest.createStack();
        assertThat(stack1).isEqualTo(stack1);
        assertThat(stack2).isEqualTo(stack2);
        assertThat(stack2).isEqualTo(stack1);
        assertThat(stack1).isEqualTo(stack2);
    }

    @Test
    public void testHashCodeVsSameKind() {
        final DefaultThreadContextStack stack1 = createStack();
        final DefaultThreadContextStack stack2 = createStack();
        assertThat(stack2.hashCode()).isEqualTo(stack1.hashCode());
    }

    @Test
    public void testImmutableOrNullReturnsNullIfUseStackIsFalse() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(false);
        stack.clear();
        assertThat(stack.getImmutableStackOrNull()).isNull();
    }

    @Test
    public void testImmutableOrNullReturnsNullIfStackIsEmpty() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        assertThat(stack.getImmutableStackOrNull()).isNull();
    }

    @Test
    public void testImmutableOrNullReturnsCopyOfContents() {
        final DefaultThreadContextStack stack = createStack();
        assertThat(stack.isEmpty()).isFalse();
        final ContextStack actual = stack.getImmutableStackOrNull();
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(stack);
    }

    @Test
    public void testModifyingImmutableOrNullThrowsException() {
        final DefaultThreadContextStack stack = createStack();
        final int originalSize = stack.size();
        assertThat(originalSize).isGreaterThan(0);
        final ContextStack actual = stack.getImmutableStackOrNull();
        assertThat(actual).hasSize(originalSize);

        assertThatThrownBy(() -> actual.pop()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testDoesNothingIfConstructedWithUseStackIsFalse() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(false);
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg");

        // nothing was added
        assertThat(stack.isEmpty()).isTrue();
        assertThat(stack).hasSize(0);
    }

    @Test
    public void testPushAndAddIncreaseStack() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg1");
        stack.add("msg2");

        assertThat(stack).hasSize(2);
    }

    @Test
    public void testPeekReturnsLastAddedItem() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg1");
        stack.add("msg2");

        assertThat(stack).hasSize(2);
        assertThat(stack.peek()).isEqualTo("msg2");

        stack.push("msg3");
        assertThat(stack.peek()).isEqualTo("msg3");
    }

    @Test
    public void testPopRemovesLastAddedItem() {
        final DefaultThreadContextStack stack = createStack();
        assertThat(stack.getDepth()).isEqualTo(3);

        assertThat(stack.pop()).isEqualTo("msg3");
        assertThat(stack).hasSize(2);
        assertThat(stack.getDepth()).isEqualTo(2);

        assertThat(stack.pop()).isEqualTo("msg2");
        assertThat(stack).hasSize(1);
        assertThat(stack.getDepth()).isEqualTo(1);

        assertThat(stack.pop()).isEqualTo("msg1");
        assertThat(stack).hasSize(0);
        assertThat(stack.getDepth()).isEqualTo(0);
    }

    @Test
    public void testAsList() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");

        assertThat(stack.asList()).isEqualTo(Arrays.asList("msg1", "msg2", "msg3"));
    }

    @Test
    public void testTrim() {
        final DefaultThreadContextStack stack = createStack();

        stack.trim(1);
        assertThat(stack).hasSize(1);
        assertThat(stack.peek()).isEqualTo("msg1");
    }

    @Test
    public void testCopy() {
        final DefaultThreadContextStack stack = createStack();

        final ThreadContextStack copy = stack.copy();
        assertThat(copy).hasSize(3);
        assertThat(copy.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();

        // clearing stack does not affect copy
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        assertThat(copy).hasSize(3); // not affected
        assertThat(copy.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();

        // adding to copy does not affect stack
        copy.add("other");
        assertThat(copy).hasSize(4); // not affected
        assertThat(stack.isEmpty()).isTrue();

        // adding to stack does not affect copy
        stack.push("newStackMsg");
        assertThat(stack).hasSize(1);
        assertThat(copy).hasSize(4); // not affected

        // clearing copy does not affect stack
        copy.clear();
        assertThat(copy.isEmpty()).isTrue();
        assertThat(stack).hasSize(1);
    }

    @Test
    public void testClear() {
        final DefaultThreadContextStack stack = createStack();

        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
    }

    /**
     * @return
     */
    static DefaultThreadContextStack createStack() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");
        assertThat(stack).hasSize(3);
        return stack;
    }

    @Test
    public void testContains() {
        final DefaultThreadContextStack stack = createStack();

        assertThat(stack.contains("msg1")).isTrue();
        assertThat(stack.contains("msg2")).isTrue();
        assertThat(stack.contains("msg3")).isTrue();
    }

    @Test
    public void testIteratorReturnsInListOrderNotStackOrder() {
        final DefaultThreadContextStack stack = createStack();

        final Iterator<String> iter = stack.iterator();
        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo("msg1");
        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo("msg2");
        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo("msg3");
        assertThat(iter.hasNext()).isFalse();
    }

    @Test
    public void testToArray() {
        final DefaultThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        assertThat(stack.toArray()).isEqualTo(expecteds);
    }

    @Test
    public void testToArrayTArray() {
        final DefaultThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        final String[] result = new String[3];
        assertThat(stack.toArray(result)).isEqualTo(expecteds);
        assertThat(stack.toArray(result)).isSameAs(result);
    }

    @Test
    public void testRemove() {
        final DefaultThreadContextStack stack = createStack();
        assertThat(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();

        stack.remove("msg1");
        assertThat(stack).hasSize(2);
        assertThat(stack.containsAll(Arrays.asList("msg2", "msg3"))).isTrue();
        assertThat(stack.peek()).isEqualTo("msg3");

        stack.remove("msg3");
        assertThat(stack).hasSize(1);
        assertThat(stack.containsAll(Collections.singletonList("msg2"))).isTrue();
        assertThat(stack.peek()).isEqualTo("msg2");
    }

    @Test
    public void testContainsAll() {
        final DefaultThreadContextStack stack = createStack();

        assertThat(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();
    }

    @Test
    public void testAddAll() {
        final DefaultThreadContextStack stack = createStack();

        stack.addAll(Arrays.asList("msg4", "msg5"));
        assertThat(stack).hasSize(5);
        assertThat(stack.contains("msg1")).isTrue();
        assertThat(stack.contains("msg2")).isTrue();
        assertThat(stack.contains("msg3")).isTrue();
        assertThat(stack.contains("msg4")).isTrue();
        assertThat(stack.contains("msg5")).isTrue();
    }

    @Test
    public void testRemoveAll() {
        final DefaultThreadContextStack stack = createStack();

        stack.removeAll(Arrays.asList("msg1", "msg3"));
        assertThat(stack).hasSize(1);
        assertThat(stack.contains("msg1")).isFalse();
        assertThat(stack.contains("msg2")).isTrue();
        assertThat(stack.contains("msg3")).isFalse();
    }

    @Test
    public void testRetainAll() {
        final DefaultThreadContextStack stack = createStack();

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertThat(stack).hasSize(2);
        assertThat(stack.contains("msg1")).isTrue();
        assertThat(stack.contains("msg2")).isFalse();
        assertThat(stack.contains("msg3")).isTrue();
    }

    @Test
    public void testToStringShowsListContents() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertThat(stack.toString()).isEqualTo("[]");

        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");
        assertThat(stack.toString()).isEqualTo("[msg1, msg2, msg3]");

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertThat(stack.toString()).isEqualTo("[msg1, msg3]");
    }
}
