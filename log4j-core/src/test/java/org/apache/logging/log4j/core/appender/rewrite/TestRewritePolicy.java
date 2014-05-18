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
package org.apache.logging.log4j.core.appender.rewrite;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

/**
 *
 */
@Plugin(name = "TestRewritePolicy", category = "Core", elementType = "rewritePolicy", printObject = true)
public class TestRewritePolicy implements RewritePolicy {

    @Override
    public LogEvent rewrite(final LogEvent source) {

        return new Log4jLogEvent(source.getLoggerName(), source.getMarker(), source.getLoggerFQCN(), source.getLevel(),
            source.getMessage(), source.getThrown(), source.getContextMap(), source.getContextStack(),
            source.getThreadName(), source.getSource(), source.getTimeMillis());
    }

    @PluginFactory
    public static TestRewritePolicy createPolicy() {
        return new TestRewritePolicy();
    }
}
