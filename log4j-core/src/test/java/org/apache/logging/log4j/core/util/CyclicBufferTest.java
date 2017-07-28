package org.apache.logging.log4j.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CyclicBufferTest {

    @Test
    public void testCyclicBuffer() {
        final CyclicBuffer<Integer> buffer = new CyclicBuffer<>(Integer.class, 3);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        assertFalse(buffer.isEmpty());
        Integer[] items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 1);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 3);
        assertTrue(buffer.isEmpty());
    }

}
