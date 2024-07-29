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
package org.apache.logging.log4j.core.jmx.internal;

import org.apache.logging.log4j.util.PropertiesUtil;

// WARNING!
// This class must be free of any dependencies to the `java.management` module!
// Otherwise, `isJmxDisabled()` call sites would unnecessarily require the `java.management` module.
// For details, see: https://github.com/apache/logging-log4j2/issues/2774

public final class JmxUtil {

    public static boolean isJmxDisabled() {
        return PropertiesUtil.getProperties().getBooleanProperty("log4j2.disable.jmx", true);
    }

    private JmxUtil() {}
}
