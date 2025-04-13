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
package org.apache.logging.log4j.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class MutableThreadContextStackTest {

    @Test
    void testEmptyIfConstructedWithEmptyList() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        assertTrue(stack.isEmpty());
    }

    @Test
    void testConstructorCopiesListContents() {
        final List<String> initial = Arrays.asList("a", "b", "c");
        final MutableThreadContextStack stack = new MutableThreadContextStack(initial);
        assertFalse(stack.isEmpty());
        assertTrue(stack.containsAll(initial));
    }

    @Test
    void testPushAndAddIncreaseStack() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");

        assertEquals(2, stack.size());
    }

    @Test
    void testPeekReturnsLastAddedItem() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");

        assertEquals(2, stack.size());
        assertEquals("msg2", stack.peek());

        stack.push("msg3");
        assertEquals("msg3", stack.peek());
    }

    @Test
    void testPopRemovesLastAddedItem() {
        final MutableThreadContextStack stack = createStack();
        assertEquals(3, stack.getDepth());

        assertEquals("msg3", stack.pop());
        assertEquals(2, stack.size());
        assertEquals(2, stack.getDepth());

        assertEquals("msg2", stack.pop());
        assertEquals(1, stack.size());
        assertEquals(1, stack.getDepth());

        assertEquals("msg1", stack.pop());
        assertEquals(0, stack.size());
        assertEquals(0, stack.getDepth());
    }

    @Test
    void testAsList() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");

        assertEquals(Arrays.asList("msg1", "msg2", "msg3"), stack.asList());
    }

    @Test
    void testTrim() {
        final MutableThreadContextStack stack = createStack();

        stack.trim(1);
        assertEquals(1, stack.size());
        assertEquals("msg1", stack.peek());
    }

    @Test
    void testCopy() {
        final MutableThreadContextStack stack = createStack();

        final ThreadContextStack copy = stack.copy();
        assertEquals(3, copy.size());
        assertTrue(copy.containsAll(Arrays.asList("msg1", "msg2", "msg3")));

        // clearing stack does not affect copy
        stack.clear();
        assertTrue(stack.isEmpty());
        assertEquals(3, copy.size()); // not affected
        assertTrue(copy.containsAll(Arrays.asList("msg1", "msg2", "msg3")));

        // adding to copy does not affect stack
        copy.add("other");
        assertEquals(4, copy.size()); // not affected
        assertTrue(stack.isEmpty());

        // adding to stack does not affect copy
        stack.push("newStackMsg");
        assertEquals(1, stack.size());
        assertEquals(4, copy.size()); // not affected

        // clearing copy does not affect stack
        copy.clear();
        assertTrue(copy.isEmpty());
        assertEquals(1, stack.size());
    }

    @Test
    void testClear() {
        final MutableThreadContextStack stack = createStack();

        stack.clear();
        assertTrue(stack.isEmpty());
    }

    @Test
    void testEqualsVsSameKind() {
        final MutableThreadContextStack stack1 = createStack();
        final MutableThreadContextStack stack2 = createStack();
        assertEquals(stack1, stack1);
        assertEquals(stack2, stack2);
        assertEquals(stack1, stack2);
        assertEquals(stack2, stack1);
    }

    @Test
    void testHashCodeVsSameKind() {
        final MutableThreadContextStack stack1 = createStack();
        final MutableThreadContextStack stack2 = createStack();
        assertEquals(stack1.hashCode(), stack2.hashCode());
    }

    /**
     * @return
     */
    static MutableThreadContextStack createStack() {
        final MutableThreadContextStack stack1 = new MutableThreadContextStack(new ArrayList<>());
        stack1.clear();
        assertTrue(stack1.isEmpty());
        stack1.push("msg1");
        stack1.add("msg2");
        stack1.push("msg3");
        assertEquals(3, stack1.size());
        return stack1;
    }

    @Test
    void testContains() {
        final MutableThreadContextStack stack = createStack();

        assertTrue(stack.contains("msg1"));
        assertTrue(stack.contains("msg2"));
        assertTrue(stack.contains("msg3"));
    }

    @Test
    void testIteratorReturnsInListOrderNotStackOrder() {
        final MutableThreadContextStack stack = createStack();

        final Iterator<String> iter = stack.iterator();
        assertTrue(iter.hasNext());
        assertEquals("msg1", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("msg2", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("msg3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    void testToArray() {
        final MutableThreadContextStack stack = createStack();

        final String[] expecteds = {"msg1", "msg2", "msg3"};
        assertArrayEquals(expecteds, stack.toArray());
    }

    @Test
    void testToArrayTArray() {
        final MutableThreadContextStack stack = createStack();

        final String[] expecteds = {"msg1", "msg2", "msg3"};
        final String[] result = new String[3];
        assertArrayEquals(expecteds, stack.toArray(result));
        assertSame(result, stack.toArray(result));
    }

    @Test
    void testRemove() {
        final MutableThreadContextStack stack = createStack();
        assertTrue(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3")));

        stack.remove("msg1");
        assertEquals(2, stack.size());
        assertTrue(stack.containsAll(Arrays.asList("msg2", "msg3")));
        assertEquals("msg3", stack.peek());

        stack.remove("msg3");
        assertEquals(1, stack.size());
        assertTrue(stack.contains("msg2"));
        assertEquals("msg2", stack.peek());
    }

    @Test
    void testContainsAll() {
        final MutableThreadContextStack stack = createStack();

        assertTrue(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3")));
    }

    @Test
    void testAddAll() {
        final MutableThreadContextStack stack = createStack();

        stack.addAll(Arrays.asList("msg4", "msg5"));
        assertEquals(5, stack.size());
        assertTrue(stack.contains("msg1"));
        assertTrue(stack.contains("msg2"));
        assertTrue(stack.contains("msg3"));
        assertTrue(stack.contains("msg4"));
        assertTrue(stack.contains("msg5"));
    }

    @Test
    void testRemoveAll() {
        final MutableThreadContextStack stack = createStack();

        stack.removeAll(Arrays.asList("msg1", "msg3"));
        assertEquals(1, stack.size());
        assertFalse(stack.contains("msg1"));
        assertTrue(stack.contains("msg2"));
        assertFalse(stack.contains("msg3"));
    }

    @Test
    void testRetainAll() {
        final MutableThreadContextStack stack = createStack();

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertEquals(2, stack.size());
        assertTrue(stack.contains("msg1"));
        assertFalse(stack.contains("msg2"));
        assertTrue(stack.contains("msg3"));
    }

    @Test
    void testToStringShowsListContents() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>());
        assertEquals("[]", stack.toString());

        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");
        assertEquals("[msg1, msg2, msg3]", stack.toString());

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertEquals("[msg1, msg3]", stack.toString());
    }

    @Test
    void testIsFrozenIsFalseByDefault() {
        assertFalse(new MutableThreadContextStack().isFrozen());
        assertFalse(createStack().isFrozen());
    }

    @Test
    void testIsFrozenIsTrueAfterCallToFreeze() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        assertFalse(stack.isFrozen());
        stack.freeze();
        assertTrue(stack.isFrozen());
    }

    @Test
    void testAddAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, () -> stack.addAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    void testAddOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, () -> stack.add("a"));
    }

    @Test
    void testClearOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, stack::clear);
    }

    @Test
    void testPopOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, stack::pop);
    }

    @Test
    void testPushOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, () -> stack.push("a"));
    }

    @Test
    void testRemoveOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, () -> stack.remove("a"));
    }

    @Test
    void testRemoveAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, () -> stack.removeAll(Arrays.asList("a", "b")));
    }

    @Test
    void testRetainAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, () -> stack.retainAll(Arrays.asList("a", "b")));
    }

    @Test
    void testTrimOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        assertThrows(UnsupportedOperationException.class, () -> stack.trim(3));
    }
}
