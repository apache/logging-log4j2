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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.MarkerManager;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;

/**
 *
 */
public class Log4jMarkerFactory implements IMarkerFactory {

    private final ConcurrentMap<String, Marker> markerMap = new ConcurrentHashMap<String, Marker>();

    /**
     * Return a Log4j Marker that is compatible with SLF4J.
     * @param name The name of the Marker.
     * @return A Marker.
     */
    @Override
    public Marker getMarker(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Marker name must not be null");
        }
        Marker marker = markerMap.get(name);
        if (marker != null) {
            return marker;
        }
        final org.apache.logging.log4j.Marker log4jMarker = MarkerManager.getMarker(name);
        marker = new Log4jMarker(log4jMarker);
        final Marker existing = markerMap.putIfAbsent(name, marker);
        return existing == null ? marker : existing;
    }

    /**
     * Returns true if the Marker exists.
     * @param name The Marker name.
     * @return true if the Marker exists, false otherwise.
     */
    @Override
    public boolean exists(final String name) {
        return markerMap.containsKey(name);
    }

    /**
     * Log4j does not support detached Markers. This method always returns false.
     * @param name The Marker name.
     * @return false
     */
    @Override
    public boolean detachMarker(final String name) {
        return false;
    }

    /**
     * Log4j does not support detached Markers for performance reasons. The returned Marker is attached.
     * @param name The Marker name.
     * @return
     */
    @Override
    public Marker getDetachedMarker(final String name) {
        return getMarker(name);
    }


}
