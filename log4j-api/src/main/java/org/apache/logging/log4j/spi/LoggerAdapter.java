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
package org.apache.logging.log4j.spi;

import java.io.Closeable;

/**
 * A basic registry for {@link LoggerContext} objects and their associated external
 * Logger classes. This registry should not be used for Log4j Loggers; it is instead used for creating bridges to
 * other external log systems.
 *
 * @param <L> the external logger class for this registry (e.g., {@code org.slf4j.Logger})
 * @since 2.1
 */
public interface LoggerAdapter<L> extends Closeable {

    /**
     * Gets a named logger. Implementations should defer to the abstract methods in {@link AbstractLoggerAdapter}.
     *
     * @param name the name of the logger to get
     * @return the named logger
     */
    L getLogger(String name);
}
