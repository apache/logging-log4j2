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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Applications create Markers by using the Marker Manager. All Markers created by this Manager are
 * immutable.
 */
public final class MarkerManager {

    private static ConcurrentMap<String, Marker> markerMap = new ConcurrentHashMap<String, Marker>();

    private MarkerManager() {
    }

    /**
     * Retrieve a Marker or create a Marker that has no parent.
     * @param name The name of the Marker.
     * @return The Marker with the specified name.
     */
    public static Marker getMarker(String name) {
        markerMap.putIfAbsent(name, new Log4JMarker(name));
        return markerMap.get(name);
    }

    /**
     * Retrieves or creates a Marker with the specified parent. The parent must have been previously created.
     * @param name The name of the Marker.
     * @param parent The name of the parent Marker.
     * @return The Marker with the specified name.
     * @throws IllegalArgumentException if the parent Marker does not exist.
     */
    public static Marker getMarker(String name, String parent) {
        Marker parentMarker = markerMap.get(parent);
        if (parentMarker == null) {
            throw new IllegalArgumentException("Parent Marker " + parent + " has not been defined");
        }
        return getMarker(name, parentMarker);
    }

    /**
     * Retrieves or creates a Marker with the specified parent.
     * @param name The name of the Marker.
     * @param parent The parent Marker.
     * @return The Marker with the specified name.
     */
    public static Marker getMarker(String name, Marker parent) {
        markerMap.putIfAbsent(name, new Log4JMarker(name, parent));
        return markerMap.get(name);
    }

    /**
     * The actual Marker implementation.
     */
    private static class Log4JMarker implements Marker {

        private static final long serialVersionUID = 100L;

        private String name;
        private Marker parent;

        public Log4JMarker(String name) {
            this.name = name;
        }

        public Log4JMarker(String name, Marker parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return this.name;
        }

        public Marker getParent() {
            return this.parent;
        }

        public boolean isInstanceOf(Marker m) {
            if (m == null) {
                throw new IllegalArgumentException("A marker parameter is required");
            }
            Marker test = this;
            do {
                if (test == m) {
                    return true;
                }
                test = test.getParent();
            } while (test != null);
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Marker marker = (Marker) o;

            if (name != null ? !name.equals(marker.getName()) : marker.getName() != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}
