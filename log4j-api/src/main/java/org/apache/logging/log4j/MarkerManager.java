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
package org.apache.logging.log4j;

import com.google.errorprone.annotations.InlineMe;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * Applications create Markers by using the Marker Manager. All Markers created by this Manager are immutable.
 */
public final class MarkerManager {

    private static final ConcurrentMap<String, Marker> MARKERS = new ConcurrentHashMap<>();

    private MarkerManager() {
        // do nothing
    }

    /**
     * Clears all markers.
     */
    public static void clear() {
        MARKERS.clear();
    }

    /**
     * Tests existence of the given marker.
     *
     * @param key the marker name
     * @return true if the marker exists.
     * @since 2.4
     */
    public static boolean exists(final String key) {
        return MARKERS.containsKey(key);
    }

    /**
     * Retrieves a Marker or create a Marker that has no parent.
     *
     * @param name The name of the Marker.
     * @return The Marker with the specified name.
     * @throws IllegalArgumentException if the argument is {@code null}
     */
    public static Marker getMarker(final String name) {
        return MARKERS.computeIfAbsent(name, Log4jMarker::new);
    }

    /**
     * Retrieves or creates a Marker with the specified parent. The parent must have been previously created.
     *
     * @param name The name of the Marker.
     * @param parent The name of the parent Marker.
     * @return The Marker with the specified name.
     * @throws IllegalArgumentException if the parent Marker does not exist.
     * @deprecated Use the Marker add or set methods to add parent Markers. Will be removed by final GA release.
     */
    @Deprecated
    public static Marker getMarker(final String name, final String parent) {
        final Marker parentMarker = MARKERS.get(parent);
        if (parentMarker == null) {
            throw new IllegalArgumentException("Parent Marker " + parent + " has not been defined");
        }
        return getMarker(name).addParents(parentMarker);
    }

    /**
     * Retrieves or creates a Marker with the specified parent.
     *
     * @param name The name of the Marker.
     * @param parent The parent Marker.
     * @return The Marker with the specified name.
     * @throws IllegalArgumentException if any argument is {@code null}
     * @deprecated Use the Marker add or set methods to add parent Markers. Will be removed by final GA release.
     */
    @InlineMe(
            replacement = "MarkerManager.getMarker(name).addParents(parent)",
            imports = "org.apache.logging.log4j.MarkerManager")
    @Deprecated
    public static Marker getMarker(final String name, final Marker parent) {
        return getMarker(name).addParents(parent);
    }

    /**
     * <em>Consider this class private, it is only public to satisfy Jackson for XML and JSON IO.</em>
     * <p>
     * The actual Marker implementation.
     * </p>
     * <p>
     * <em>Internal note: We could make this class package private instead of public if the class
     * {@code org.apache.logging.log4j.core.jackson.MarkerMixIn}
     * is moved to this package and would of course stay in its current module.</em>
     * </p>
     */
    public static class Log4jMarker implements Marker, StringBuilderFormattable {

        private static final long serialVersionUID = 100L;

        private final String name;

        private volatile Marker[] parents;

        /**
         * Required by JAXB and Jackson for XML and JSON IO.
         */
        @SuppressWarnings("unused")
        private Log4jMarker() {
            this.name = null;
            this.parents = null;
        }

        /**
         * Constructs a new Marker.
         *
         * @param name the name of the Marker.
         * @throws IllegalArgumentException if the argument is {@code null}
         */
        public Log4jMarker(final String name) {
            // we can't store null references in a ConcurrentHashMap as it is, not to mention that a null Marker
            // name seems rather pointless. To get an "anonymous" Marker, just use an empty string.
            requireNonNull(name, "Marker name cannot be null.");
            this.name = name;
            this.parents = null;
        }

        // TODO: use java.util.concurrent

        @Override
        public synchronized Marker addParents(final Marker... parentMarkers) {
            requireNonNull(parentMarkers, "A parent marker must be specified");
            // It is not strictly necessary to copy the variable here but it should perform better than
            // Accessing a volatile variable multiple times.
            final Marker[] localParents = this.parents;
            // Don't add a parent that is already in the hierarchy.
            int count = 0;
            int size = parentMarkers.length;
            if (localParents != null) {
                for (final Marker parent : parentMarkers) {
                    if (!(contains(parent, localParents) || parent.isInstanceOf(this))) {
                        ++count;
                    }
                }
                if (count == 0) {
                    return this;
                }
                size = localParents.length + count;
            }
            final Marker[] markers = new Marker[size];
            if (localParents != null) {
                // It's perfectly OK to call arraycopy in a synchronized context; it's still faster
                // noinspection CallToNativeMethodWhileLocked
                System.arraycopy(localParents, 0, markers, 0, localParents.length);
            }
            int index = localParents == null ? 0 : localParents.length;
            for (final Marker parent : parentMarkers) {
                if (localParents == null || !(contains(parent, localParents) || parent.isInstanceOf(this))) {
                    markers[index++] = parent;
                }
            }
            this.parents = markers;
            return this;
        }

        @Override
        public synchronized boolean remove(final Marker parent) {
            requireNonNull(parent, "A parent marker must be specified");
            final Marker[] localParents = this.parents;
            if (localParents == null) {
                return false;
            }
            final int localParentsLength = localParents.length;
            if (localParentsLength == 1) {
                if (localParents[0].equals(parent)) {
                    parents = null;
                    return true;
                }
                return false;
            }
            int index = 0;
            final Marker[] markers = new Marker[localParentsLength - 1];
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < localParentsLength; i++) {
                final Marker marker = localParents[i];
                if (!marker.equals(parent)) {
                    if (index == localParentsLength - 1) {
                        // no need to swap array
                        return false;
                    }
                    markers[index++] = marker;
                }
            }
            parents = markers;
            return true;
        }

        @Override
        public Marker setParents(final Marker... markers) {
            if (markers == null || markers.length == 0) {
                this.parents = null;
            } else {
                final Marker[] array = new Marker[markers.length];
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
        public Marker[] getParents() {
            final Marker[] parentsSnapshot = parents;
            if (parentsSnapshot == null) {
                return null;
            }
            return Arrays.copyOf(parentsSnapshot, parentsSnapshot.length);
        }

        @Override
        public boolean hasParents() {
            return this.parents != null;
        }

        @Override
        @PerformanceSensitive({"allocation", "unrolled"})
        public boolean isInstanceOf(final Marker marker) {
            requireNonNull(marker, "A marker parameter is required");
            if (this == marker) {
                return true;
            }
            final Marker[] localParents = parents;
            if (localParents != null) {
                // With only one or two parents the for loop is slower.
                final int localParentsLength = localParents.length;
                if (localParentsLength == 1) {
                    return checkParent(localParents[0], marker);
                }
                if (localParentsLength == 2) {
                    return checkParent(localParents[0], marker) || checkParent(localParents[1], marker);
                }
                // noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < localParentsLength; i++) {
                    final Marker localParent = localParents[i];
                    if (checkParent(localParent, marker)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        @PerformanceSensitive({"allocation", "unrolled"})
        public boolean isInstanceOf(final String markerName) {
            requireNonNull(markerName, "A marker name is required");
            if (markerName.equals(this.getName())) {
                return true;
            }
            // Use a real marker for child comparisons. It is faster than comparing the names.
            final Marker marker = MARKERS.get(markerName);
            if (marker == null) {
                return false;
            }
            final Marker[] localParents = parents;
            if (localParents != null) {
                final int localParentsLength = localParents.length;
                if (localParentsLength == 1) {
                    return checkParent(localParents[0], marker);
                }
                if (localParentsLength == 2) {
                    return checkParent(localParents[0], marker) || checkParent(localParents[1], marker);
                }
                // noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < localParentsLength; i++) {
                    final Marker localParent = localParents[i];
                    if (checkParent(localParent, marker)) {
                        return true;
                    }
                }
            }

            return false;
        }

        @PerformanceSensitive({"allocation", "unrolled"})
        private static boolean checkParent(final Marker parent, final Marker marker) {
            if (parent == marker) {
                return true;
            }
            final Marker[] localParents =
                    parent instanceof Log4jMarker ? ((Log4jMarker) parent).parents : parent.getParents();
            if (localParents != null) {
                final int localParentsLength = localParents.length;
                if (localParentsLength == 1) {
                    return checkParent(localParents[0], marker);
                }
                if (localParentsLength == 2) {
                    return checkParent(localParents[0], marker) || checkParent(localParents[1], marker);
                }
                // noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < localParentsLength; i++) {
                    final Marker localParent = localParents[i];
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
        @PerformanceSensitive("allocation")
        private static boolean contains(final Marker parent, final Marker... localParents) {
            // performance tests showed a normal for loop is slightly faster than a for-each loop on some platforms
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0, localParentsLength = localParents.length; i < localParentsLength; i++) {
                final Marker marker = localParents[i];
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
            return name.equals(marker.getName());
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            // FIXME: might want to use an initial capacity; the default is 16 (or str.length() + 16)
            final StringBuilder sb = new StringBuilder();
            formatTo(sb);
            return sb.toString();
        }

        @Override
        public void formatTo(final StringBuilder sb) {
            sb.append(name);
            final Marker[] localParents = parents;
            if (localParents != null) {
                addParentInfo(sb, localParents);
            }
        }

        @PerformanceSensitive("allocation")
        private static void addParentInfo(final StringBuilder sb, final Marker... parents) {
            sb.append("[ ");
            boolean first = true;
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0, parentsLength = parents.length; i < parentsLength; i++) {
                final Marker marker = parents[i];
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(marker.getName());
                final Marker[] p = marker instanceof Log4jMarker ? ((Log4jMarker) marker).parents : marker.getParents();
                if (p != null) {
                    addParentInfo(sb, p);
                }
            }
            sb.append(" ]");
        }
    }

    // this method wouldn't be necessary if Marker methods threw an NPE instead of an IAE for null values ;)
    private static void requireNonNull(final Object obj, final String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
