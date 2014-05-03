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
package org.apache.logging.log4j;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class MarkerTest {

    @Test
    public void markerTest() {
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(parent);
        final Marker test2 = MarkerManager.getMarker("TEST2").addParents(parent);
        assertTrue("TEST1 is not an instance of PARENT", test1.isInstanceOf(parent));
        assertTrue("TEST2 is not an instance of PARENT", test2.isInstanceOf(parent));
    }

    @Test
    public void multipleParentsTest() {
        final Marker parent1 = MarkerManager.getMarker("PARENT1");
        final Marker parent2 = MarkerManager.getMarker("PARENT2");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(parent1, parent2);
        final Marker test2 = MarkerManager.getMarker("TEST2").addParents(parent1, parent2);
        assertTrue("TEST1 is not an instance of PARENT1", test1.isInstanceOf(parent1));
        assertTrue("TEST1 is not an instance of PARENT2", test1.isInstanceOf(parent2));
        assertTrue("TEST2 is not an instance of PARENT1", test2.isInstanceOf(parent1));
        assertTrue("TEST2 is not an instance of PARENT2", test2.isInstanceOf(parent2));
    }

    @Test
    public void addToExistingParentsTest() {
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker existing = MarkerManager.getMarker("EXISTING");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(existing);
        test1.addParents(parent);
        assertTrue("TEST1 is not an instance of PARENT", test1.isInstanceOf(parent));
        assertTrue("TEST1 is not an instance of EXISTING", test1.isInstanceOf(existing));
    }


    @Test
    public void duplicateParentsTest() {
        final Marker parent = MarkerManager.getMarker("PARENT");
        final Marker existing = MarkerManager.getMarker("EXISTING");
        final Marker test1 = MarkerManager.getMarker("TEST1").setParents(existing);
        test1.addParents(parent);
        Marker[] parents = test1.getParents();
        test1.addParents(existing);
        assertTrue("duplicate add allowed", parents.length == test1.getParents().length);
        test1.addParents(existing, MarkerManager.getMarker("EXTRA"));
        assertTrue("incorrect add", parents.length + 1 == test1.getParents().length);
        assertTrue("TEST1 is not an instance of PARENT", test1.isInstanceOf(parent));
        assertTrue("TEST1 is not an instance of EXISTING", test1.isInstanceOf(existing));
    }
}
