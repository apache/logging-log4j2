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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.status.StatusLogger;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;

/**
 * Log4j/SLF4J bridge to create SLF4J Markers based on name or based on existing SLF4J Markers.
 */
public class Log4jMarkerFactory implements IMarkerFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ConcurrentMap<String, Marker> markerMap = new ConcurrentHashMap<>();
    private final Set<String> detachedMarkers = new CopyOnWriteArraySet<>();

    /**
     * Returns a Log4j Marker that is compatible with SLF4J.
     *
     * @param name The name of the Marker.
     * @return A Marker.
     */
    @Override
    public Marker getMarker(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Marker name must not be null");
        }
        final Marker marker = markerMap.get(name);
        if (marker != null) {
            return marker;
        }
        final org.apache.logging.log4j.Marker log4jMarker = MarkerManager.getMarker(name);
        return addMarkerIfAbsent(name, log4jMarker);
    }

    private Marker addMarkerIfAbsent(final String name, final org.apache.logging.log4j.Marker log4jMarker) {
        final Marker marker = new Log4jMarker(log4jMarker);
        final Marker existing = markerMap.putIfAbsent(name, marker);
        return existing == null ? marker : existing;
    }

    /**
     * Returns a Log4j Marker converted from an existing custom SLF4J Marker.
     * If the marker has been detached (see {@link Log4jMarkerFactory#detachMarker(String)}),
     * returns the unmodified marker.
     *
     * @param marker The SLF4J Marker to convert.
     * @return Same input Marker is it has been detached, a converted Log4j/SLF4J Marker otherwise.
     * @since 2.1
     */
    public Marker getMarker(final Marker marker) {
        if (marker == null) {
            throw new IllegalArgumentException("Marker must not be null");
        }
        final String name = marker.getName();
        if (detachedMarkers.contains(name)) {
            return marker;
        }
        final Marker m = markerMap.get(name);
        if (m != null) {
            return m;
        }
        return addMarkerIfAbsent(name, convertMarker(marker));
    }

    private static org.apache.logging.log4j.Marker convertMarker(final Marker original) {
        if (original == null) {
            throw new IllegalArgumentException("Marker must not be null");
        }
        return convertMarker(original, new ArrayList<>());
    }

    private static org.apache.logging.log4j.Marker convertMarker(final Marker original,
                                                                 final Collection<Marker> visited) {
        final org.apache.logging.log4j.Marker marker = MarkerManager.getMarker(original.getName());
        if (original.hasReferences()) {
            final Iterator<Marker> it = original.iterator();
            while (it.hasNext()) {
                final Marker next = it.next();
                if (visited.contains(next)) {
                    LOGGER.warn("Found a cycle in Marker [{}]. Cycle will be broken.", next.getName());
                } else {
                    visited.add(next);
                    marker.addParents(convertMarker(next, visited));
                }
            }
        }
        return marker;
    }

    /**
     * Returns true if the Marker exists.
     *
     * @param name The Marker name.
     * @return {@code true} if the Marker exists, {@code false} otherwise.
     */
    @Override
    public boolean exists(final String name) {
        return markerMap.containsKey(name);
    }

    @Override
    public boolean detachMarker(final String name) {
        if (name == null) {
            return false;
        }
        detachedMarkers.add(name);
        return true;
    }

    @Override
    public Marker getDetachedMarker(final String name) {
        return new Log4jMarker(new MarkerManager.Log4jMarker(name));
    }

}
