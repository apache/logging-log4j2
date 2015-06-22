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
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.MarkerManager;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.impl.StaticMarkerBinder;

/**
 * Log4j/SLF4J {@link org.slf4j.Marker} type bridge.
 */
public class Log4jMarker implements Marker {

    public static final long serialVersionUID = 1590472L;

    private final IMarkerFactory factory = StaticMarkerBinder.SINGLETON.getMarkerFactory();

    private final org.apache.logging.log4j.Marker marker;

    /**
     * Constructs a Log4jMarker using an existing Log4j {@link org.apache.logging.log4j.Marker}.
     * @param marker The Log4j Marker upon which to base this Marker.
     */
    public Log4jMarker(final org.apache.logging.log4j.Marker marker) {
        this.marker = marker;
    }

    public org.apache.logging.log4j.Marker getLog4jMarker() {
        return marker;
    }

    @Override
    public void add(final Marker marker) {
        final Marker m = factory.getMarker(marker.getName());
        this.marker.addParents(((Log4jMarker)m).getLog4jMarker());
    }

    @Override
	public boolean remove(final Marker marker) {
		return marker != null ? this.marker.remove(MarkerManager.getMarker(marker.getName())) : false;
	}

    @Override
    public String getName() {
        return marker.getName();
    }

    @Override
    public boolean hasReferences() {
        return marker.hasParents();
    }

    @Override
    public boolean hasChildren() {
        return marker.hasParents();
    }

    @Override
    public Iterator<Marker> iterator() {
        final List<Marker> parents = new ArrayList<>();
        for (final org.apache.logging.log4j.Marker m : this.marker.getParents()) {
            parents.add(factory.getMarker(m.getName()));
        }
        return parents.iterator();
    }

    @Override
	public boolean contains(final org.slf4j.Marker marker) {
		if (marker == null) {
			throw new IllegalArgumentException();
		}
		return this.marker.isInstanceOf(marker.getName());
	}

    @Override
	public boolean contains(final String s) {
		return s != null ? this.marker.isInstanceOf(s) : false;
	}
}
