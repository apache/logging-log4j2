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

import java.util.Arrays;
import java.util.Iterator;

import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultThreadContextStackTest {

    @Before
    public void before() {
        // clear the thread-local map
        new DefaultThreadContextMap(true).clear();
    }

    @Test
    public void testEqualsVsSameKind() {
        final DefaultThreadContextStack stack1 = createStack();
        final DefaultThreadContextStack stack2 = createStack();
        assertEquals(stack1, stack1);
        assertEquals(stack2, stack2);
        assertEquals(stack1, stack2);
        assertEquals(stack2, stack1);
    }

    @Test
    public void testEqualsVsMutable() {
        final DefaultThreadContextStack stack1 = createStack();
        final MutableThreadContextStack stack2 = MutableThreadContextStackTest.createStack();
        assertEquals(stack1, stack1);
        assertEquals(stack2, stack2);
        assertEquals(stack1, stack2);
        assertEquals(stack2, stack1);
    }

    @Test
    public void testHashCodeVsSameKind() {
        final DefaultThreadContextStack stack1 = createStack();
        final DefaultThreadContextStack stack2 = createStack();
        assertEquals(stack1.hashCode(), stack2.hashCode());
    }

    @Test
    public void testImmutableOrNullReturnsNullIfUseStackIsFalse() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(false);
        stack.clear();
        assertEquals(null, stack.getImmutableStackOrNull());
    }

    @Test
    public void testImmutableOrNullReturnsNullIfStackIsEmpty() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertTrue(stack.isEmpty());
        assertEquals(null, stack.getImmutableStackOrNull());
    }

    @Test
    public void testImmutableOrNullReturnsCopyOfContents() {
        final DefaultThreadContextStack stack = createStack();
        assertTrue(!stack.isEmpty());
        final ContextStack actual = stack.getImmutableStackOrNull();
        assertNotNull(actual);
        assertEquals(stack, actual);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyingImmutableOrNullThrowsException() {
        final DefaultThreadContextStack stack = createStack();
        final int originalSize = stack.size();
        assertTrue(originalSize > 0);
        final ContextStack actual = stack.getImmutableStackOrNull();
        assertEquals(originalSize, actual.size());

        actual.pop();
    }

    @Test
    public void testDoesNothingIfConstructedWithUseStackIsFalse() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(false);
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg");

        // nothing was added
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    public void testPushAndAddIncreaseStack() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");

        assertEquals(2, stack.size());
    }

    @Test
    public void testPeekReturnsLastAddedItem() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
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
        final DefaultThreadContextStack stack = createStack();
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
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");

        assertEquals(Arrays.asList("msg1", "msg2", "msg3"), stack.asList());
    }

    @Test
    public void testTrim() {
        final DefaultThreadContextStack stack = createStack();

        stack.trim(1);
        assertEquals(1, stack.size());
        assertEquals("msg1", stack.peek());
    }

    @Test
    public void testCopy() {
        final DefaultThreadContextStack stack = createStack();

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
        final DefaultThreadContextStack stack = createStack();

        stack.clear();
        assertTrue(stack.isEmpty());
    }

    /**
     * @return
     */
    static DefaultThreadContextStack createStack() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertTrue(stack.isEmpty());
        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");
        assertEquals(3, stack.size());
        return stack;
    }

    @Test
    public void testContains() {
        final DefaultThreadContextStack stack = createStack();

        assertTrue(stack.contains("msg1"));
        assertTrue(stack.contains("msg2"));
        assertTrue(stack.contains("msg3"));
    }

    @Test
    public void testIteratorReturnsInListOrderNotStackOrder() {
        final DefaultThreadContextStack stack = createStack();

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
        final DefaultThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        assertArrayEquals(expecteds, stack.toArray());
    }

    @Test
    public void testToArrayTArray() {
        final DefaultThreadContextStack stack = createStack();

        final String[] expecteds = { "msg1", "msg2", "msg3" };
        final String[] result = new String[3];
        assertArrayEquals(expecteds, stack.toArray(result));
        assertSame(result, stack.toArray(result));
    }

    @Test
    public void testRemove() {
        final DefaultThreadContextStack stack = createStack();
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
        final DefaultThreadContextStack stack = createStack();

        assertTrue(stack.containsAll(Arrays.asList("msg1", "msg2", "msg3")));
    }

    @Test
    public void testAddAll() {
        final DefaultThreadContextStack stack = createStack();

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
        final DefaultThreadContextStack stack = createStack();

        stack.removeAll(Arrays.asList("msg1", "msg3"));
        assertEquals(1, stack.size());
        assertFalse(stack.contains("msg1"));
        assertTrue(stack.contains("msg2"));
        assertFalse(stack.contains("msg3"));
    }

    @Test
    public void testRetainAll() {
        final DefaultThreadContextStack stack = createStack();

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertEquals(2, stack.size());
        assertTrue(stack.contains("msg1"));
        assertFalse(stack.contains("msg2"));
        assertTrue(stack.contains("msg3"));
    }

    @Test
    public void testToStringShowsListContents() {
        final DefaultThreadContextStack stack = new DefaultThreadContextStack(true);
        stack.clear();
        assertEquals("[]", stack.toString());

        stack.push("msg1");
        stack.add("msg2");
        stack.push("msg3");
        assertEquals("[msg1, msg2, msg3]", stack.toString());

        stack.retainAll(Arrays.asList("msg1", "msg3"));
        assertEquals("[msg1, msg3]", stack.toString());
    }
}
