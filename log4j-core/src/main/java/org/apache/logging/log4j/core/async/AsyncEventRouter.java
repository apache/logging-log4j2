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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;

/**
 * Routing rule for deciding whether to log an event on the current thread, the background thread, or not at all.
 *
 * @since 2.6
 */
public interface AsyncEventRouter {

    /**
     * Returns the appropriate route for this routing rule, given the specified parameters.
     *
     * @param backgroundThreadId the thread ID of the background thread. Can be compared with the current thread's ID.
     * @param level the level of the log event
     * @return the appropriate route for this routing rule
     */
    EventRoute getRoute(final long backgroundThreadId, final Level level);
}
