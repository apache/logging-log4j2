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

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/*
 * Never triggers and is handy for edge-cases in tests for example.
 *
 * @since 2.11.1
 */
@Plugin(name = "NoOpTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
public class NoOpTriggeringPolicy extends AbstractTriggeringPolicy {

    public static final NoOpTriggeringPolicy INSTANCE = new NoOpTriggeringPolicy();

    @PluginFactory
    public static NoOpTriggeringPolicy createPolicy() {
        return INSTANCE;
    }

    @Override
    public void initialize(final RollingFileManager manager) {
        // NoOp
    }

    @Override
    public boolean isTriggeringEvent(final LogEvent logEvent) {
        // Never triggers.
        return false;
    }
}
