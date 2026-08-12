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

import org.apache.logging.log4j.common.util.Source;

/**
 * Watches for changes in a configuration {@link Source} and performs an action when it is modified.
 *
 * @since 3.0.0
 */
public interface Watcher extends FileWatcher {

    /**
     * Returns the time the source was last modified or {@code 0} if it is not available.
     *
     * @return the time the source was last modified
     */
    long getLastModified();

    /**
     * Called when the watcher is registered for the given source.
     *
     * @param source the source that is being watched
     */
    void watching(Source source);
}
