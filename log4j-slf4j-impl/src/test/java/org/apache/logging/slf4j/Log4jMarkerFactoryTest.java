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
package org.apache.logging.slf4j;

import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import java.util.Collections;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Log4jMarkerFactoryTest {

    @Test
    void getMarker() {
        // Given
        Log4jMarkerFactory sut = new Log4jMarkerFactory();

        // When
        Marker firstResult = sut.getMarker("FOO");
        Marker secondResult = sut.getMarker("FOO");

        // Then, second result is cached
        assertSame(firstResult, secondResult);
        assertTrue(sut.exists("FOO"));
    }

    @Test
    void getDetachedMarker() {
        // Given
        Log4jMarkerFactory sut = new Log4jMarkerFactory();

        // When
        Marker firstResult = sut.getDetachedMarker("FOO");
        Marker secondResult = sut.getDetachedMarker("FOO");

        // Then, second result is not cached (a detached marker)
        assertNotSame(firstResult, secondResult);
        assertFalse(sut.exists("FOO"));
    }

    @Test
    void detachMarker() {
        // Given
        Log4jMarkerFactory sut = new Log4jMarkerFactory();

        // When
        boolean result = sut.detachMarker("WHATEVER");

        // Then
        assertTrue(result);
    }

    @Test
    void detachedMarkerIsUntouched() {
        // Given
        StructuredArg structuredArg = new StructuredArg(42);
        Log4jMarkerFactory sut = new Log4jMarkerFactory();

        // we ask to detach markers with this same
        sut.detachMarker(structuredArg.getName());

        // When, we call the internal method getMarker (not part of the SLF4J org.slf4j.IMarkerFactory interface)
        Marker result = sut.getMarker(structuredArg);

        // Then, the original marker is not converted
        assertSame(structuredArg, result);
    }

    @Test
    void detachedMarkerIsNotCached() {
        // Given
        StructuredArg first = new StructuredArg(42);
        StructuredArg second = new StructuredArg(10);
        Log4jMarkerFactory sut = new Log4jMarkerFactory();

        // we ask to detach markers with this same
        sut.detachMarker(first.getName());

        // When
        Marker firstResult = sut.getMarker(first);
        Marker secondResult = sut.getMarker(second);

        // Then, the original marker is not converted
        assertSame(firstResult, first);
        assertSame(secondResult, second);
        assertNotSame(firstResult, secondResult);
    }

    @Test
    void nonDetachedMarkerIsConverted() {
        // Given
        StructuredArg structuredArg = new StructuredArg(42);
        Log4jMarkerFactory sut = new Log4jMarkerFactory();

        // When
        Marker result = sut.getMarker(structuredArg);

        // Then, the original marker has been converted (and cached)
        assertNotSame(structuredArg, result);
        assertTrue(result instanceof Log4jMarker); // converted, not the original marker anymore
    }

    @Test
    void comparisonDetachedAndNonDetachedMarkers() {
        // Given
        Log4jMarkerFactory sut = new Log4jMarkerFactory();

        // When
        Marker nonDetachedMarker = sut.getMarker("name");
        Marker detachedMarker = sut.getDetachedMarker("name");

        // Then, the original marker has been converted (and cached)
        assertNotSame(nonDetachedMarker, detachedMarker);
        assertTrue(nonDetachedMarker.equals(detachedMarker));
        assertTrue(detachedMarker.equals(nonDetachedMarker));
    }

    private static class StructuredArg implements org.slf4j.Marker {

        // some random data, markers are used to pass extra "stateful" information
        // that cannot be cached
        @SuppressWarnings("unused")
        private final int data;

        public StructuredArg(int data) {
            this.data = data;
        }

        @Override
        public String getName() {
            return "STRUCTURED_ARG";
        }

        @Override
        public void add(Marker reference) {
            // no-op
        }

        @Override
        public boolean remove(Marker reference) {
            return false;
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public boolean hasReferences() {
            return false;
        }

        @Override
        public Iterator<Marker> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public boolean contains(Marker other) {
            return false;
        }

        @Override
        public boolean contains(String name) {
            return false;
        }
    }

}
