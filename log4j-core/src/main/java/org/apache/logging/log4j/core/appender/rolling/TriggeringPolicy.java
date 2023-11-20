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
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.core.LogEvent;

/**
 * A <code>TriggeringPolicy</code> controls the conditions under which rollover
 * occurs. Such conditions include time of day, file size, an
 * external event, the log request or a combination thereof.
 *
 * @see AbstractTriggeringPolicy
 */
public interface TriggeringPolicy /* TODO 3.0: extends LifeCycle */ {

    /**
     * Initializes this triggering policy.
     * @param manager The RollingFileManager.
     */
    void initialize(final RollingFileManager manager);

    /**
     * Determines if a rollover may be appropriate at this time.  If
     * true is returned, RolloverPolicy.rollover will be called but it
     * can determine that a rollover is not warranted.
     *
     * @param logEvent   A reference to the current log event.
     * @return true if a rollover should occur.
     */
    boolean isTriggeringEvent(final LogEvent logEvent);
}
