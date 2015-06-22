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

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class MarkerTest {

	private static final String CHILD_MAKER_NAME = MarkerTest.class.getSimpleName() + "-TEST";
	private static final String PARENT_MARKER_NAME = MarkerTest.class.getSimpleName() + "-PARENT";

	@Before
	@After
	public void clearMarkers() {
		MarkerManager.clear();
	}

	@Test
	public void testAddSameMarker() {
		String childMarkerName = CHILD_MAKER_NAME + "-ASM";
		String parentMakerName = PARENT_MARKER_NAME + "-ASM";
		final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
		final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
		slf4jMarker.add(slf4jParent);
		slf4jMarker.add(slf4jParent);
		final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
		final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
		assertTrue(String.format("%s (log4jMarker=%s) is not an instance of %s (log4jParent=%s) in Log4j",
				childMarkerName, parentMakerName, log4jMarker, log4jParent), log4jMarker.isInstanceOf(log4jParent));
		assertTrue(String.format("%s (slf4jMarker=%s) is not an instance of %s (log4jParent=%s) in SLF4J",
				childMarkerName, parentMakerName, slf4jMarker, slf4jParent), slf4jMarker.contains(slf4jParent));
	}

	@Test
	public void testAddNullMarker() {
		String childMarkerName = CHILD_MAKER_NAME + "-ANM";
		String parentMakerName = PARENT_MARKER_NAME + "-ANM";
		final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
		final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
		slf4jMarker.add(slf4jParent);
		final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
		final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
		final Log4jMarker log4jSlf4jParent = new Log4jMarker(log4jParent);
		final Log4jMarker log4jSlf4jMarker = new Log4jMarker(log4jMarker);
		final org.slf4j.Marker nullMarker = null;
		try {
			log4jSlf4jParent.add(nullMarker);
			fail("Expected " + IllegalArgumentException.class.getName());
		} catch (final IllegalArgumentException e) {
			// expected
		}
		try {
			log4jSlf4jMarker.add(nullMarker);
			fail("Expected " + IllegalArgumentException.class.getName());
		} catch (final IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testContainsNullMarker() {
		String childMarkerName = CHILD_MAKER_NAME + "-CM";
		String parentMakerName = PARENT_MARKER_NAME + "-CM";
		final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
		final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
		slf4jMarker.add(slf4jParent);
		final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
		final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
		final Log4jMarker log4jSlf4jParent = new Log4jMarker(log4jParent);
		final Log4jMarker log4jSlf4jMarker = new Log4jMarker(log4jMarker);
		final org.slf4j.Marker nullMarker = null;
		try {
			Assert.assertFalse(log4jSlf4jParent.contains(nullMarker));
			fail("Expected " + IllegalArgumentException.class.getName());
		} catch (final IllegalArgumentException e) {
			// expected
		}
		try {
			Assert.assertFalse(log4jSlf4jMarker.contains(nullMarker));
			fail("Expected " + IllegalArgumentException.class.getName());
		} catch (final IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testContainsNullString() {
		String childMarkerName = CHILD_MAKER_NAME + "-CS";
		String parentMakerName = PARENT_MARKER_NAME + "-CS";
		final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
		final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
		slf4jMarker.add(slf4jParent);
		final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
		final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
		final Log4jMarker log4jSlf4jParent = new Log4jMarker(log4jParent);
		final Log4jMarker log4jSlf4jMarker = new Log4jMarker(log4jMarker);
		final String nullStr = null;
		Assert.assertFalse(log4jSlf4jParent.contains(nullStr));
		Assert.assertFalse(log4jSlf4jMarker.contains(nullStr));
	}

	@Test
	public void testAddMarker() {
		String childMakerName = CHILD_MAKER_NAME + "-AM";
		final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMakerName);
		String parentMarkerName = PARENT_MARKER_NAME + "-AM";
		final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMarkerName);
		slf4jMarker.add(slf4jParent);
		final Marker log4jParent = MarkerManager.getMarker(parentMarkerName);
		final Marker log4jMarker = MarkerManager.getMarker(childMakerName);

		assertTrue("Incorrect Marker class", slf4jMarker instanceof Log4jMarker);
		assertTrue(String.format("%s (log4jMarker=%s) is not an instance of %s (log4jParent=%s) in Log4j",
				childMakerName, parentMarkerName, log4jMarker, log4jParent), log4jMarker.isInstanceOf(log4jParent));
		assertTrue(String.format("%s (slf4jMarker=%s) is not an instance of %s (log4jParent=%s) in SLF4J",
				childMakerName, parentMarkerName, slf4jMarker, slf4jParent), slf4jMarker.contains(slf4jParent));
	}

	@Test
	public void testRemoveNullMarker() {
		String childMakerName = CHILD_MAKER_NAME + "-CM";
		String parentMakerName = PARENT_MARKER_NAME + "-CM";
		final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMakerName);
		final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
		slf4jMarker.add(slf4jParent);
		final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
		final Marker log4jMarker = MarkerManager.getMarker(childMakerName);
		final Log4jMarker log4jSlf4jParent = new Log4jMarker(log4jParent);
		final Log4jMarker log4jSlf4jMarker = new Log4jMarker(log4jMarker);
		final org.slf4j.Marker nullMarker = null;
		Assert.assertFalse(log4jSlf4jParent.remove(nullMarker));
		Assert.assertFalse(log4jSlf4jMarker.remove(nullMarker));
	}

}
