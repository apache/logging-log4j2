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
package org.apache.logging.log4j.core.pattern;

/**
 * Interface that all PatternConverters must implement.
 */
public interface PatternConverter {

    /**
     * Main plugin category for PatternConverter plugins.
     *
     * @since 2.1
     */
    String CATEGORY = "Converter";

    /**
     * Formats an object into a string buffer.
     *
     * @param obj        event to format, may not be null.
     * @param toAppendTo string buffer to which the formatted event will be appended.  May not be null.
     */
    void format(Object obj, StringBuilder toAppendTo);

    /**
     * Returns the name of the converter.
     * @return The name of the converter.
     */
    String getName();

    /**
     * This method returns the CSS style class that should be applied to
     * the LoggingEvent passed as parameter, which can be null.
     *
     * @param e null values are accepted
     * @return the name of the conversion pattern
     */
    String getStyleClass(Object e);
}
