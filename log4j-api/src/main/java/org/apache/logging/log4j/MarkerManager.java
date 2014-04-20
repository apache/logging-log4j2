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

import java.util.Arrays;
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
     * @deprecated Use the Marker add or set methods to add parent Markers. Will be removed by final GA release.
     */
    @Deprecated
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
     * @deprecated Use the Marker add or set methods to add parent Markers. Will be removed by final GA release.
     */
    @Deprecated
    public static Marker getMarker(final String name, final Marker parent) {
        markerMap.putIfAbsent(name, new Log4jMarker(name));
        return markerMap.get(name).add(parent);
    }
    /**
     * The actual Marker implementation.
     */
    private static class Log4jMarker implements Marker {

        private static final long serialVersionUID = 100L;

        private final String name;
        private volatile Marker[] parents;

        public Log4jMarker(final String name) {
            this.name = name;
            this.parents = null;
        }

        @Override
        public synchronized Marker add(Marker parent) {
            if (parent == null) {
                throw new IllegalArgumentException("A parent marker must be specified");
            }
            // It is not strictly necessary to copy the variable here but it should perform better than
            // Accessing a volatile variable multiple times.
            Marker[] localParents = this.parents;
            // Don't add a parent that is already in the hierarchy.
            if (localParents != null && (contains(parent, localParents) || parent.isInstanceOf(this))) {
                return this;
            }
            int size = localParents == null ? 1 : localParents.length + 1;
            Marker[] markers = new Marker[size];
            if (localParents != null) {
                // It's perfectly OK to call arraycopy in a synchronized context; it's still faster
                //noinspection CallToNativeMethodWhileLocked
                System.arraycopy(localParents, 0, markers, 0, localParents.length);
            }
            markers[size - 1] = parent;
            this.parents = markers;
            return this;
        }

        @Override
        public synchronized boolean remove(Marker parent) {
            if (parent == null) {
                throw new IllegalArgumentException("A parent marker must be specified");
            }
            Marker[] localParents = this.parents;
            if (localParents == null) {
                return false;
            }
            if (localParents.length == 1) {
                if (localParents[0].equals(parent)) {
                    parents = null;
                    return true;
                }
                return false;
            }
            int index = 0;
            Marker[] markers = new Marker[localParents.length - 1];
            for (Marker marker : localParents) {
                if (!marker.equals(parent)) {
                    if (index == localParents.length - 1) {
                        return false;
                    }
                    markers[index++] = marker;
                }
            }
            parents = markers;
            return true;
        }

        @Override
        public Marker setParents(Marker... markers) {
            if (markers == null || markers.length == 0) {
                this.parents = null;
            } else {
                Marker[] array = new Marker[markers.length];
                System.arraycopy(markers, 0, array, 0, markers.length);
                this.parents = array;
            }
            return this;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Marker getParent() {
            return this.parents != null ? parents[0] : null;
        }

        @Override
        public Marker[] getParents() {
            if (this.parents == null) {
                return null;
            }
            return Arrays.copyOf(this.parents, this.parents.length);
        }

        @Override
        public boolean hasParents() {
            return this.parents == null;
        }

        @Override
        public boolean isInstanceOf(final Marker marker) {
            if (marker == null) {
                throw new IllegalArgumentException("A marker parameter is required");
            }
            if (this == marker) {
                return true;
            }
            Marker[] localParents = parents;
            if (localParents != null) {
                // With only one or two parents the for loop is slower.
                if (localParents.length == 1) {
                    return checkParent(localParents[0], marker);
                }
                if (localParents.length == 2) {
                    return checkParent(localParents[0], marker) || checkParent(localParents[1], marker);
                }
                for (final Marker localParent : localParents) {
                    if (checkParent(localParent, marker)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean isInstanceOf(final String markerName) {
            if (markerName == null) {
                throw new IllegalArgumentException("A marker name is required");
            }
            if (markerName.equals(this.getName())) {
                return true;
            }
            // Use a real marker for child comparisons. It is faster than comparing the names.
            Marker marker = markerMap.get(markerName);
            if (marker == null) {
                throw new IllegalArgumentException("No marker exists with the name " + markerName);
            }
            Marker[] localParents = parents;
            if (localParents != null) {
                if (localParents.length == 1) {
                    return checkParent(localParents[0], marker);
                }
                if (localParents.length == 2) {
                    return checkParent(localParents[0], marker) || checkParent(localParents[1], marker);
                }
                for (final Marker localParent : localParents) {
                    if (checkParent(localParent, marker)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean checkParent(Marker parent, Marker marker) {
            if (parent == marker) {
                return true;
            }
            Marker[] localParents = parent instanceof Log4jMarker ? ((Log4jMarker)parent).parents : parent.getParents();
            if (localParents != null) {
                if (localParents.length == 1) {
                    return checkParent(localParents[0], marker);
                }
                if (localParents.length == 2) {
                    return checkParent(localParents[0], marker) || checkParent(localParents[1], marker);
                }
                for (final Marker localParent : localParents) {
                    if (checkParent(localParent, marker)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /*
         * Called from add while synchronized.
         */
        private boolean contains(Marker parent, Marker... localParents) {

            for (Marker marker : localParents) {
                if (marker == parent) {
                    return true;
                }
            }
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
            Marker[] localParents = parents;
            if (localParents != null) {
                addParentInfo(localParents, sb);
            }
            return sb.toString();
        }

        private void addParentInfo(Marker[] parents, StringBuilder sb) {
            sb.append("[ ");
            boolean first = true;
            for (Marker marker : parents) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(marker.getName());
                Marker[] p = marker instanceof Log4jMarker ? ((Log4jMarker)marker).parents : marker.getParents();
                if (p != null) {
                    addParentInfo(p, sb);
                }
            }
            sb.append(" ]");
        }
    }
}
