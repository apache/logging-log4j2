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

import java.util.Iterator;

import org.slf4j.Marker;

/**
 * Test Marker that may contain no reference/parent Markers.
 * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-793">LOG4J2-793</a>
 */
public class CustomFlatMarker implements Marker {
    private static final long serialVersionUID = -4115520883240247266L;

    private final String name;

    public CustomFlatMarker(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void add(final Marker reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(final Marker reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasChildren() {
        return hasReferences();
    }

    @Override
    public boolean hasReferences() {
        return false;
    }

    @Override
    public Iterator<Marker> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(final Marker other) {
        return false;
    }

    @Override
    public boolean contains(final String name) {
        return false;
    }
}
