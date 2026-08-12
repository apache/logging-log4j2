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
package org.apache.logging.log4j.config.spi;

import java.io.InputStream;
import java.net.URI;

/**
 * Service provider interface representing the source of a logging configuration.
 * <p>
 * This interface mirrors the essential read-only accessors of
 * {@code org.apache.logging.log4j.core.config.ConfigurationSource} without depending on core types.
 * </p>
 *
 * @since 3.0.0
 */
public interface ConfigurationSourceSPI {

    /**
     * Returns the input stream from which the configuration was loaded.
     *
     * @return the configuration input stream
     */
    InputStream getInputStream();

    /**
     * Returns a string describing the configuration source location, or {@code null} if unavailable.
     *
     * @return the configuration source location
     */
    String getLocation();

    /**
     * Returns a URI representing the configuration resource, or {@code null} if unavailable.
     *
     * @return the configuration source URI
     */
    URI getURI();
}
