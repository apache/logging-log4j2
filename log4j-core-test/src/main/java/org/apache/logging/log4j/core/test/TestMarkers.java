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
package org.apache.logging.log4j.core.test;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

/**
 * Markers useful in tests.
 */
public class TestMarkers {

    public static final Marker LIFE_CYCLE = MarkerManager.getMarker("LIFECYCLE");
    public static final Marker TEST = MarkerManager.getMarker("TEST");
    public static final Marker TEST_RULE = MarkerManager.getMarker("TEST_RULE").addParents(TEST);
    public static final Marker TEST_RULE_LIFE_CYCLE = MarkerManager.getMarker("TEST_RULE_LIFE_CYCLE")
            .addParents(TEST_RULE)
            .addParents(LIFE_CYCLE);
}
