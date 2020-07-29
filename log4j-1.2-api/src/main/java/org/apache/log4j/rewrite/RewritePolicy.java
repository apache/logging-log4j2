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
package org.apache.log4j.rewrite;

import org.apache.log4j.spi.LoggingEvent;

/**
 * This interface is implemented to provide a rewrite
 * strategy for RewriteAppender.  RewriteAppender will
 * call the rewrite method with a source logging event.
 * The strategy may return that event, create a new event
 * or return null to suppress the logging request.
 */
public interface RewritePolicy {
    /**
     * Rewrite a logging event.
     * @param source a logging event that may be returned or
     * used to create a new logging event.
     * @return a logging event or null to suppress processing.
     */
    LoggingEvent rewrite(final LoggingEvent source);
}
