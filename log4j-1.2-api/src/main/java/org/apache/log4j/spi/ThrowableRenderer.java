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
package org.apache.log4j.spi;

/**
 * Implemented by classes that render instances of java.lang.Throwable (exceptions and errors) into a string
 * representation.
 *
 * @since 1.2.16
 */
public interface ThrowableRenderer {

    /**
     * Render Throwable.
     *
     * @param t throwable, may not be null.
     * @return String representation.
     */
    public String[] doRender(Throwable t);
}
