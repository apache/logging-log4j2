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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class MutableThreadContextStackTest {

    @Test
    public void testEmptyIfConstructedWithEmptyList() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<String>());
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testConstructorCopiesListContents() {
        final List<String> initial = Arrays.asList("a", "b", "c");
        final MutableThreadContextStack stack = new MutableThreadContextStack(initial);
        assertFalse(stack.isEmpty());
        assertTrue(stack.containsAll(initial));
    }

    @Test
    public void testPushAndAddIncreaseStack() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<String>());
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");

        assertEquals(2, stack.size());
    }

    @Test
    public void testPeekReturnsLastAddedItem() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<String>());
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
    public void testPopRemovesLastAddedItem() {
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
    public void testAsList() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<String>());
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");

        assertEquals(Arrays.asList("msg1", "msg2", "msg3"), stack.asList());
    }

    @Test
    public void testTrim() {
        final MutableThreadContextStack stack = createStack();

        stack.trim(1);
        assertEquals(1, stack.size());
        assertEquals("msg1", stack.peek());
    }

    @Test
    public void testCopy() {
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
    public void testClear() {
        final MutableThreadContextStack stack = createStack();

        stack.clear();
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testEqualsVsSameKind() {
        final MutableThreadContextStack stack1 = createStack();
        final MutableThreadContextStack stack2 = createStack();
        assertEquals(stack1, stack1);
        assertEquals(stack2, stack2);
        assertEquals(stack1, stack2);
        assertEquals(stack2, stack1);
    }

    @Test
    public void testHashCodeVsSameKind() {
        final MutableThreadContextStack stack1 = createStack();
        final MutableThreadContextStack stack2 = createStack();
        assertEquals(stack1.hashCode(), stack2.hashCode());
    }

    /**
     * @return
     */
    static MutableThreadContextStack createStack() {
        final MutableThreadContextStack stack1 = new MutableThreadContextStack(new ArrayList<String>());
        stack1.clear();
        assertTrue(stack1.isEmpty());
        stack1.push("msg1");
        stack1.add("msg2");
        stack1.push("msg3");
        assertEquals(3, stack1.size());
        return stack1;
    }

    @Test
    public void testContains() {
        final MutableThreadContextStack stack = createStack();

        assertTrue(stack.contains("msg1"));
        assertTrue(stack.contains("msg2"));
        assertTrue(stack.contains("msg3"));
    }

    @Test
    public void testIteratorReturnsInListOrderNotStackOrder() {
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
    public void testToArray() {
        final MutableThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        assertArrayEquals(expecteds, stack.toArray());
    }

    @Test
    public void testToArrayTArray() {
        final MutableThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        final String[] result = new String[3];
        assertArrayEquals(expecteds, stack.toArray(result));
        assertSame(result, stack.toArray(result));
    }

    @Test
    public void testRemove() {
        final MutableThreadContextStack stack = createStack();
        assertTrue(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3")));

        stack.remove("msg1");
        assertEquals(2, stack.size());
        assertTrue(stack.containsAll(Arrays.asList("msg2", "msg3")));
        assertEquals("msg3", stack.peek());

        stack.remove("msg3");
        assertEquals(1, stack.size());
        assertTrue(stack.containsAll(Arrays.asList("msg2")));
        assertEquals("msg2", stack.peek());
    }

    @Test
    public void testContainsAll() {
        final MutableThreadContextStack stack = createStack();

        assertTrue(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3")));
    }

    @Test
    public void testAddAll() {
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
    public void testRemoveAll() {
        final MutableThreadContextStack stack = createStack();

        stack.removeAll(Arrays.asList("msg1", "msg3"));
        assertEquals(1, stack.size());
        assertFalse(stack.contains("msg1"));
        assertTrue(stack.contains("msg2"));
        assertFalse(stack.contains("msg3"));
    }

    @Test
    public void testRetainAll() {
        final MutableThreadContextStack stack = createStack();

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertEquals(2, stack.size());
        assertTrue(stack.contains("msg1"));
        assertFalse(stack.contains("msg2"));
        assertTrue(stack.contains("msg3"));
    }

    @Test
    public void testToStringShowsListContents() {
        final MutableThreadContextStack stack = new MutableThreadContextStack(new ArrayList<String>());
        assertEquals("[]", stack.toString());

        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");
        assertEquals("[msg1, msg2, msg3]", stack.toString());

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertEquals("[msg1, msg3]", stack.toString());
    }

    @Test
    public void testIsFrozenIsFalseByDefault() {
        assertFalse(new MutableThreadContextStack().isFrozen());
        assertFalse(createStack().isFrozen());
    }

    @Test
    public void testIsFrozenIsTrueAfterCallToFreeze() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        assertFalse(stack.isFrozen());
        stack.freeze();
        assertTrue(stack.isFrozen());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.addAll(Arrays.asList("a", "b", "c"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.add("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPopOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.pop();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPushOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.push("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.remove("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.removeAll(Arrays.asList("a", "b"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.retainAll(Arrays.asList("a", "b"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTrimOnFrozenStackThrowsException() {
        final MutableThreadContextStack stack = new MutableThreadContextStack();
        stack.freeze();
        stack.trim(3);
    }
}
