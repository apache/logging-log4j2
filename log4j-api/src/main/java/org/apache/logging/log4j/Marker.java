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

import java.io.Serializable;

/**
 *  Markers are objects that are used to add easily filterable information to log messages.
 *
 *  Markers can be hierarchical - each Marker may have a parent. This allows for broad categories
 *  being subdivided into more specific categories. An example might be a Marker named "Error" with
 *  children named "SystemError" and "ApplicationError".
 */
public interface Marker extends Serializable {

    /**
     * Returns the name of this Marker.
     * @return The name of the Marker.
     */
    String getName();

    /**
     * Returns a list of parents of this Marker.
     * @return The parent Markers or {@code null} if this Marker has no parents.
     */
    Marker[] getParents();

    /**
     * Indicates whether this Marker has references to any other Markers.
     * @return {@code true} if the Marker has parent Markers
     */
    boolean hasParents();

    /**
     * Checks whether this Marker is an instance of the specified Marker.
     * @param m The Marker to check.
     * @return {@code true} if this Marker or one of its ancestors is the specified Marker, {@code false} otherwise.
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    boolean isInstanceOf(Marker m);

    /**
     * Checks whether this Marker is an instance of the specified Marker.
     * @param name The name of the Marker.
     * @return {@code true} if this Marker or one of its ancestors matches the specified name, {@code false} otherwise.
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    boolean isInstanceOf(String name);

    /**
     * Adds a Marker as a parent to this Marker.
     * @param markers The parent markers to add.
     * @return The current Marker object, thus allowing multiple adds to be concatenated.
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    Marker addParents(Marker... markers);

    /**
     * Replaces the set of parent Markers with the provided Markers.
     * @param markers The new set of parent Markers or {@code null} to clear the parents.
     * @return The current Marker object.
     */
    Marker setParents(Marker... markers);

    /**
     * Removes the specified Marker as a parent of this Marker.
     * @param marker The marker to remove.
     * @return {@code true} if the marker was removed.
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    boolean remove(Marker marker);
}
