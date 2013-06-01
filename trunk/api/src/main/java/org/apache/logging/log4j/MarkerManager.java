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
    public static Marker getMarker(final String name) {
        markerMap.putIfAbsent(name, new Log4jMarker(name));
        return markerMap.get(name);
    }

    /**
     * Retrieves or creates a Marker with the specified parent. The parent must have been previously created.
     * @param name The name of the Marker.
     * @param parent The name of the parent Marker.
     * @return The Marker with the specified name.
     * @throws IllegalArgumentException if the parent Marker does not exist.
     */
    public static Marker getMarker(final String name, final String parent) {
        final Marker parentMarker = markerMap.get(parent);
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
    public static Marker getMarker(final String name, final Marker parent) {
        markerMap.putIfAbsent(name, new Log4jMarker(name, parent));
        return markerMap.get(name);
    }

    /**
     * The actual Marker implementation.
     */
    private static class Log4jMarker implements Marker {

        private static final long serialVersionUID = 100L;

        private final String name;
        private final Marker parent;

        public Log4jMarker(final String name) {
            this.name = name;
            this.parent = null;
        }

        public Log4jMarker(final String name, final Marker parent) {
            this.name = name;
            this.parent = parent;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Marker getParent() {
            return this.parent;
        }

        @Override
        public boolean isInstanceOf(final Marker m) {
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
        public boolean isInstanceOf(final String name) {
            if (name == null) {
                throw new IllegalArgumentException("A marker name is required");
            }
            Marker toTest = this;
            do {
                if (name.equals(toTest.getName())) {
                    return true;
                }
                toTest = toTest.getParent();
            } while (toTest != null);
            return false;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !(o instanceof Marker)) {
                return false;
            }

            final Marker marker = (Marker) o;

            if (name != null ? !name.equals(marker.getName()) : marker.getName() != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(name);
            if (parent != null) {
                Marker m = parent;
                sb.append("[ ");
                boolean first = true;
                while (m != null) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(m.getName());
                    first = false;
                    m = m.getParent();
                }
                sb.append(" ]");
            }
            return sb.toString();
        }
    }
}
