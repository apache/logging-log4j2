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
package org.apache.log4j.legacy.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Provide access to Log4j Core Logger methods.
 */
public final class CategoryUtil {

    private CategoryUtil() {
    }

    public static boolean isAdditive(Logger logger) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            return ((org.apache.logging.log4j.core.Logger) logger).isAdditive();
        }
        return false;
    }

    public static void setAdditivity(Logger logger, boolean additivity) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            ((org.apache.logging.log4j.core.Logger) logger).setAdditive(additivity);
        }
    }

    public static Logger getParent(Logger logger) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            return ((org.apache.logging.log4j.core.Logger) logger).getParent();

        }
        return null;
    }

    public static LoggerContext getLoggerContext(Logger logger) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            return ((org.apache.logging.log4j.core.Logger) logger).getContext();
        }
        return null;
    }

    public static void setLevel(Logger logger, Level level) {
        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            ((org.apache.logging.log4j.core.Logger) logger).setLevel(level);

        }
    }
}
