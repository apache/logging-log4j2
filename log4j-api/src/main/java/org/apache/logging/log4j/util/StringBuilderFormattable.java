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
package org.apache.logging.log4j.util;

/**
 * Objects that implement this interface can be converted to text, ideally without allocating temporary objects.
 *
 * @since 2.6
 */
public interface StringBuilderFormattable {

    /**
     * Writes a text representation of this object into the specified {@code StringBuilder}, ideally without allocating
     * temporary objects.
     *
     * @param buffer the StringBuilder to write into
     */
    void formatTo(StringBuilder buffer);
}
