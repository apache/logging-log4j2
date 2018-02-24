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

package org.apache.logging.log4j.message;

import java.util.Map;

import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * A {@link StringMapMessage} typed to {@link String}-only values. This is like the MapMessage class before 2.9.
 * 
 * @since 2.9
 */
@PerformanceSensitive("allocation")
@AsynchronouslyFormattable
public class StringMapMessage extends MapMessage<StringMapMessage, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new instance.
     */
    public StringMapMessage() {
        super();
    }

    public StringMapMessage(final StackTraceElement source) {
        super(source);
    }

    /**
     * Constructs a new instance.
     * 
     * @param initialCapacity
     *            the initial capacity.
     */
    public StringMapMessage(final int initialCapacity) {
        this(null, initialCapacity);
    }

    public StringMapMessage(final StackTraceElement source, final int initialCapacity) {
        super(source, initialCapacity);
    }

    /**
     * Constructs a new instance based on an existing Map.
     * 
     * @param map
     *            The Map.
     */
    public StringMapMessage(final Map<String, String> map) {
        this(null, map);
    }

    public StringMapMessage(StackTraceElement source, final Map<String, String> map) {
        super(source, map);
    }

    /**
     * Constructs a new instance based on an existing Map.
     * @param map The Map.
     * @return A new StringMapMessage
     */
    @Override
    public StringMapMessage newInstance(final Map<String, String> map) {
        return new StringMapMessage(map);
    }
}
