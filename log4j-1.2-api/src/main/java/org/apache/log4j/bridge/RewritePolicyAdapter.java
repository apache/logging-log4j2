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
package org.apache.log4j.bridge;

import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Binds a Log4j 1.x RewritePolicy to Log4j 2.
 */
public class RewritePolicyAdapter implements org.apache.logging.log4j.core.appender.rewrite.RewritePolicy {

    private final RewritePolicy policy;

    /**
     * Constructor.
     * @param policy The Rewrite policy.
     */
    public RewritePolicyAdapter(final RewritePolicy policy) {
        this.policy = policy;
    }

    @Override
    public LogEvent rewrite(final LogEvent source) {
        final LoggingEvent event = policy.rewrite(new LogEventAdapter(source));
        return event instanceof LogEventAdapter ? ((LogEventAdapter) event).getEvent() : new LogEventWrapper(event);
    }

    public RewritePolicy getPolicy() {
        return this.policy;
    }
}
