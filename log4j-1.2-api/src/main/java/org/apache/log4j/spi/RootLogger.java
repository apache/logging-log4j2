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
package org.apache.log4j.spi;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;

/**
 * RootLogger sits at the top of the logger hierarchy. It is a regular logger except that it provides several guarantees.
 * <p>
 * First, it cannot be assigned a <code>null</code> level. Second, since root logger cannot have a parent, the
 * {@link #getChainedLevel} method always returns the value of the level field without walking the hierarchy.
 * </p>
 */
public final class RootLogger extends Logger {

    /**
     * The root logger names itself as "root". However, the root logger cannot be retrieved by name.
     */
    public RootLogger(final Level level) {
        // The Log4j 1 root logger name is "root".
        // The Log4j 2 root logger name is "".
        super("root");
        setLevel(level);
    }

    /**
     * Gets the assigned level value without walking the logger hierarchy.
     */
    public final Level getChainedLevel() {
        return getLevel();
    }

    /**
     * Sets the log level.
     *
     * Setting a null value to the level of the root logger may have catastrophic results. We prevent this here.
     *
     * @since 0.8.3
     */
    public final void setLevel(final Level level) {
        if (level == null) {
            LogLog.error("You have tried to set a null level to root.", new Throwable());
        } else {
            super.setLevel(level);
        }
    }
}
