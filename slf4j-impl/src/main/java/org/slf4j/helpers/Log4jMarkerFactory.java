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
package org.slf4j.helpers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;

/**
 *
 */
public class Log4jMarkerFactory implements IMarkerFactory {

    private final ConcurrentMap<String, Marker> markerMap = new ConcurrentHashMap<String, Marker>();

    @Override
    public Marker getMarker(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Marker name must not be null");
        }
        Marker marker = markerMap.get(name);
        if (marker != null) {
            return marker;
        }
        marker = new MarkerWrapper(name);
        final Marker existing = markerMap.putIfAbsent(name, marker);
        return existing == null ? marker : existing;
    }

    @Override
    public boolean exists(final String name) {
        return markerMap.containsKey(name);
    }

    @Override
    public boolean detachMarker(final String name) {
        return false;
    }

    @Override
    public Marker getDetachedMarker(final String name) {
        return new MarkerWrapper(name);
    }
}
