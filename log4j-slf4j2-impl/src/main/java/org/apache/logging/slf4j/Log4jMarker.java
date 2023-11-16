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
package org.apache.logging.slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.MarkerManager;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;

/**
 * Log4j/SLF4J {@link Marker} type bridge.
 */
public class Log4jMarker implements Marker {

    public static final long serialVersionUID = 1590472L;

    private final IMarkerFactory factory;

    private final org.apache.logging.log4j.Marker marker;

    /**
     * Constructs a Log4jMarker using an existing Log4j {@link org.apache.logging.log4j.Marker}.
     * @param marker The Log4j Marker upon which to base this Marker.
     */
    public Log4jMarker(final IMarkerFactory markerFactory, final org.apache.logging.log4j.Marker marker) {
        this.factory = markerFactory;
        this.marker = marker;
    }

    @Override
    public void add(final Marker marker) {
        if (marker == null) {
            throw new IllegalArgumentException();
        }
        final Marker m = factory.getMarker(marker.getName());
        this.marker.addParents(((Log4jMarker) m).getLog4jMarker());
    }

    @Override
    public boolean contains(final Marker marker) {
        if (marker == null) {
            throw new IllegalArgumentException();
        }
        return this.marker.isInstanceOf(marker.getName());
    }

    @Override
    public boolean contains(final String s) {
        return s != null ? this.marker.isInstanceOf(s) : false;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Log4jMarker)) {
            return false;
        }
        final Log4jMarker other = (Log4jMarker) obj;
        return Objects.equals(marker, other.marker);
    }

    public org.apache.logging.log4j.Marker getLog4jMarker() {
        return marker;
    }

    @Override
    public String getName() {
        return marker.getName();
    }

    @Override
    public boolean hasChildren() {
        return marker.hasParents();
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(marker);
    }

    @Override
    public boolean hasReferences() {
        return marker.hasParents();
    }

    @Override
    public Iterator<Marker> iterator() {
        final org.apache.logging.log4j.Marker[] log4jParents = this.marker.getParents();
        if (log4jParents == null) {
            return Collections.emptyIterator();
        }
        final List<Marker> parents = new ArrayList<>(log4jParents.length);
        for (final org.apache.logging.log4j.Marker m : log4jParents) {
            parents.add(factory.getMarker(m.getName()));
        }
        return parents.iterator();
    }

    @Override
    public boolean remove(final Marker marker) {
        return marker != null ? this.marker.remove(MarkerManager.getMarker(marker.getName())) : false;
    }
}
