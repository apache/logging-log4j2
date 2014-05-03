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

import org.apache.logging.log4j.MarkerManager;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.impl.StaticMarkerBinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class Log4jMarker implements Marker {

    public static final long serialVersionUID = 1590472L;

    private final IMarkerFactory factory = StaticMarkerBinder.SINGLETON.getMarkerFactory();

    private final org.apache.logging.log4j.Marker marker;

    public Log4jMarker(org.apache.logging.log4j.Marker marker) {
        this.marker = marker;
    }

    public org.apache.logging.log4j.Marker getLog4jMarker() {
        return marker;
    }

    @Override
    public void add(Marker marker) {
        Marker m = factory.getMarker(marker.getName());
        this.marker.add(((Log4jMarker)m).getLog4jMarker());
    }

    @Override
    public boolean remove(Marker marker) {
        return this.marker.remove(MarkerManager.getMarker(marker.getName()));
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
    @SuppressWarnings("rawtypes")
    public Iterator iterator() {
        List<Marker> parents = new ArrayList<Marker>();
        for (org.apache.logging.log4j.Marker m : this.marker.getParents()) {
            parents.add(factory.getMarker(m.getName()));
        }
        return parents.iterator();
    }

    @Override
    public boolean contains(org.slf4j.Marker marker) {
        return this.marker.isInstanceOf(marker.getName());
    }

    @Override
    public boolean contains(String s) {
        return this.marker.isInstanceOf(s);
    }
}
