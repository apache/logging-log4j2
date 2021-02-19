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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MutableThreadContextStackTest {

    @Test
    public void testEmptyIfConstructedWithEmptyList() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    public void testConstructorCopiesListContents() {
        final List<String> initial = Arrays.asList("a", "b", "c");
        final MutableThreadContextStack stack = new MutableThreadContextStack(initial);
        assertThat(stack.isEmpty()).isFalse();
        assertThat(stack.containsAll(initial)).isTrue();
    }

    @Test
    public void testPushAndAddIncreaseStack() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg1");
        stack.add("msg2");

        assertThat(stack.size()).isEqualTo(2);
    }

    @Test
    public void testPeekReturnsLastAddedItem() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg1");
        stack.add("msg2");

        assertThat(stack.size()).isEqualTo(2);
        assertThat(stack.peek()).isEqualTo("msg2");

        stack.push("msg3");
        assertThat(stack.peek()).isEqualTo("msg3");
    }

    @Test
    public void testPopRemovesLastAddedItem() {
        final MutableThreadContextStack stack = createStack();
        assertThat(stack.getDepth()).isEqualTo(3);

        assertThat(stack.pop()).isEqualTo("msg3");
        assertThat(stack.size()).isEqualTo(2);
        assertThat(stack.getDepth()).isEqualTo(2);

        assertThat(stack.pop()).isEqualTo("msg2");
        assertThat(stack.size()).isEqualTo(1);
        assertThat(stack.getDepth()).isEqualTo(1);

        assertThat(stack.pop()).isEqualTo("msg1");
        assertThat(stack.size()).isEqualTo(0);
        assertThat(stack.getDepth()).isEqualTo(0);
    }

    @Test
    public void testAsList() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");

        assertThat(stack.asList()).isEqualTo(Arrays.asList("msg1", "msg2", "msg3"));
    }

    @Test
    public void testTrim() {
        final MutableThreadContextStack stack = createStack();

        stack.trim(1);
        assertThat(stack.size()).isEqualTo(1);
        assertThat(stack.peek()).isEqualTo("msg1");
    }

    @Test
    public void testCopy() {
        final MutableThreadContextStack stack = createStack();

        final ThreadContextStack copy = stack.copy();
        assertThat(copy.size()).isEqualTo(3);
        assertThat(copy.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();

        // clearing stack does not affect copy
        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
        assertThat(copy.size()).isEqualTo(3); // not affected
        assertThat(copy.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();

        // adding to copy does not affect stack
        copy.add("other");
        assertThat(copy.size()).isEqualTo(4); // not affected
        assertThat(stack.isEmpty()).isTrue();

        // adding to stack does not affect copy
        stack.push("newStackMsg");
        assertThat(stack.size()).isEqualTo(1);
        assertThat(copy.size()).isEqualTo(4); // not affected

        // clearing copy does not affect stack
        copy.clear();
        assertThat(copy.isEmpty()).isTrue();
        assertThat(stack.size()).isEqualTo(1);
    }

    @Test
    public void testClear() {
        final MutableThreadContextStack stack = createStack();

        stack.clear();
        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    public void testEqualsVsSameKind() {
        final MutableThreadContextStack stack1 = createStack();
        final MutableThreadContextStack stack2 = createStack();
        assertThat(stack1).isEqualTo(stack1);
        assertThat(stack2).isEqualTo(stack2);
        assertThat(stack2).isEqualTo(stack1);
        assertThat(stack1).isEqualTo(stack2);
    }

    @Test
    public void testHashCodeVsSameKind() {
        final MutableThreadContextStack stack1 = createStack();
        final MutableThreadContextStack stack2 = createStack();
        assertThat(stack2.hashCode()).isEqualTo(stack1.hashCode());
    }

    /**
     * @return
     */
    static MutableThreadContextStack createStack() {
        final MutableThreadContextStack stack1 = new MutableThreadContextStack(new ArrayList<>());
        stack1.clear();
        assertThat(stack1.isEmpty()).isTrue();
        stack1.push("msg1");
        stack1.add("msg2");
        stack1.push("msg3");
        assertThat(stack1.size()).isEqualTo(3);
        return stack1;
    }

    @Test
    public void testContains() {
        final MutableThreadContextStack stack = createStack();

        assertThat(stack.contains("msg1")).isTrue();
        assertThat(stack.contains("msg2")).isTrue();
        assertThat(stack.contains("msg3")).isTrue();
    }

    @Test
    public void testIteratorReturnsInListOrderNotStackOrder() {
        final MutableThreadContextStack stack = createStack();

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
        final MutableThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        assertThat(stack.toArray()).isEqualTo(expecteds);
    }

    @Test
    public void testToArrayTArray() {
        final MutableThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        final String[] result = new String[3];
        assertThat(stack.toArray(result)).isEqualTo(expecteds);
        assertThat(stack.toArray(result)).isSameAs(result);
    }

    @Test
    public void testRemove() {
        final MutableThreadContextStack stack = createStack();
        assertThat(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();

        stack.remove("msg1");
        assertThat(stack.size()).isEqualTo(2);
        assertThat(stack.containsAll(Arrays.asList("msg2", "msg3"))).isTrue();
        assertThat(stack.peek()).isEqualTo("msg3");

        stack.remove("msg3");
        assertThat(stack.size()).isEqualTo(1);
        assertThat(stack.contains("msg2")).isTrue();
        assertThat(stack.peek()).isEqualTo("msg2");
    }

    @Test
    public void testContainsAll() {
        final MutableThreadContextStack stack = createStack();

        assertThat(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3"))).isTrue();
    }

    @Test
    public void testAddAll() {
        final MutableThreadContextStack stack = createStack();

        stack.addAll(Arrays.asList("msg4", "msg5"));
        assertThat(stack.size()).isEqualTo(5);
        assertThat(stack.contains("msg1")).isTrue();
        assertThat(stack.contains("msg2")).isTrue();
        assertThat(stack.contains("msg3")).isTrue();
        assertThat(stack.contains("msg4")).isTrue();
        assertThat(stack.contains("msg5")).isTrue();
    }

    @Test
    public void testRemoveAll() {
        final MutableThreadContextStack stack = createStack();

        stack.removeAll(Arrays.asList("msg1", "msg3"));
        assertThat(stack.size()).isEqualTo(1);
        assertThat(stack.contains("msg1")).isFalse();
        assertThat(stack.contains("msg2")).isTrue();
        assertThat(stack.contains("msg3")).isFalse();
    }

    @Test
    public void testRetainAll() {
        final MutableThreadContextStack stack = createStack();

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertThat(stack.size()).isEqualTo(2);
        assertThat(stack.contains("msg1")).isTrue();
        assertThat(stack.contains("msg2")).isFalse();
        assertThat(stack.contains("msg3")).isTrue();
    }

    @Test
    public void testToStringShowsListContents() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        assertThat(stack.toString()).isEqualTo("[]");

        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");
        assertThat(stack.toString()).isEqualTo("[msg1, msg2, msg3]");

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertThat(stack.toString()).isEqualTo("[msg1, msg3]");
    }

    @Test
    public void testIsFrozenIsFalseByDefault() {
        assertThat(new MutableThreadContextStack().isFrozen()).isFalse();
        assertThat(createStack().isFrozen()).isFalse();
    }

    @Test
    public void testIsFrozenIsTrueAfterCallToFreeze() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        assertThat(stack.isFrozen()).isFalse();
        stack.freeze();
        assertThat(stack.isFrozen()).isTrue();
    }

    @Test
    public void testAddAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThatThrownBy(() -> stack.addAll(Arrays.asList("a", "b", "c"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testAddOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThatThrownBy(() -> stack.add("a")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testClearOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, stack::clear);
    }

    @Test
    public void testPopOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, stack::pop);
    }

    @Test
    public void testPushOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThatThrownBy(() -> stack.push("a")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testRemoveOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThatThrownBy(() -> stack.remove("a")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testRemoveAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThatThrownBy(() -> stack.removeAll(Arrays.asList("a", "b"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testRetainAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThatThrownBy(() -> stack.retainAll(Arrays.asList("a", "b"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testTrimOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThatThrownBy(() -> stack.trim(3)).isInstanceOf(UnsupportedOperationException.class);
    }
}
