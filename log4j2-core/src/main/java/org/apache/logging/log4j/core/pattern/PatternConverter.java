/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.logging.log4j.core.pattern;


/**
 * <p>PatternConverter is an abstract class that provides the
 * formatting functionality that derived classes need.
 * <p/>
 * <p>Conversion specifiers in a conversion patterns are parsed to
 * individual PatternConverters. Each of which is responsible for
 * converting an object in a converter specific manner.
 */
public abstract class PatternConverter {
    /**
     * Converter name.
     */
    private final String name;

    /**
     * Converter style name.
     */
    private final String style;

    /**
     * Create a new pattern converter.
     *
     * @param name  name for pattern converter.
     * @param style CSS style for formatted output.
     */
    protected PatternConverter(final String name, final String style) {
        this.name = name;
        this.style = style;
    }

    /**
     * Formats an object into a string buffer.
     *
     * @param obj        event to format, may not be null.
     * @param toAppendTo string buffer to which the formatted event will be appended.  May not be null.
     */
    public abstract void format(final Object obj, final StringBuilder toAppendTo);

    /**
     * This method returns the name of the conversion pattern.
     * <p/>
     * The name can be useful to certain Layouts such as HTMLLayout.
     *
     * @return the name of the conversion pattern
     */
    public final String getName() {
        return name;
    }

    /**
     * This method returns the CSS style class that should be applied to
     * the LoggingEvent passed as parameter, which can be null.
     * <p/>
     * This information is currently used only by HTMLLayout.
     *
     * @param e null values are accepted
     * @return the name of the conversion pattern
     */
    public String getStyleClass(Object e) {
        return style;
    }
}
