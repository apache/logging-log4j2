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
package org.apache.logging.log4j.jansi;

import java.io.OutputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.jline.jansi.AnsiConsole;
import org.jspecify.annotations.Nullable;

/**
 * Uses the JAnsi library to provide standard output and error.
 */
@Plugin
@Namespace(ConsoleAppender.ConsoleStreamSupplier.NAMESPACE)
@Ordered(0)
public class JansiConsoleStreamSupplier implements ConsoleAppender.ConsoleStreamSupplier {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public @Nullable OutputStream getOutputStream(
            boolean follow, boolean direct, ConsoleAppender.Target target, PropertyEnvironment properties) {
        if (properties.getProperty(JansiProperties.class).jansiEnabled()) {
            if (follow || direct) {
                LOGGER.error(
                        "Can not use neither `follow` nor `direct` on ConsoleAppender, since JAnsi library is used.");
                return null;
            }
            return new CloseShieldOutputStream(
                    target == ConsoleAppender.Target.SYSTEM_ERR ? AnsiConsole.err() : AnsiConsole.out());
        }
        LOGGER.debug("Ignoring JAnsi library since configuration property `log4j.console.jansiEnabled` is false.");
        return null;
    }
}
