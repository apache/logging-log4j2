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
package org.apache.log4j.bridge;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.log4j.rewrite.RewritePolicy;

/**
 * Binds a Log4j 2 RewritePolicy to Log4j 1.
 */
public class RewritePolicyWrapper implements RewritePolicy {

    private final org.apache.logging.log4j.core.appender.rewrite.RewritePolicy policy;

    public RewritePolicyWrapper(org.apache.logging.log4j.core.appender.rewrite.RewritePolicy policy) {
        this.policy = policy;
    }

    @Override
    public LoggingEvent rewrite(LoggingEvent source) {
        LogEvent event = source instanceof LogEventAdapter ? ((LogEventAdapter) source).getEvent() :
                new LogEventWrapper(source);
        return new LogEventAdapter(policy.rewrite(event));
    }

    public org.apache.logging.log4j.core.appender.rewrite.RewritePolicy getPolicy() {
        return policy;
    }
}
