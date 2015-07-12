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
import org.junit.Assert;
import org.junit.Test;

public class Log4jMarkerTest {

	@Test
	public void testEquals() {
		final Marker markerA = MarkerManager.getMarker(Log4jMarkerTest.class.getName() + "-A");
		final Marker markerB = MarkerManager.getMarker(Log4jMarkerTest.class.getName() + "-B");
		final Log4jMarker marker1 = new Log4jMarker(markerA);
		final Log4jMarker marker2 = new Log4jMarker(markerA);
		final Log4jMarker marker3 = new Log4jMarker(markerB);
		Assert.assertEquals(marker1, marker2);
		Assert.assertNotEquals(marker1, null);
		Assert.assertNotEquals(null, marker1);
		Assert.assertNotEquals(marker1, marker3);
	}
}
