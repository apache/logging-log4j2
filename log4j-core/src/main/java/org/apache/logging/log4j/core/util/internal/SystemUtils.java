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
package org.apache.logging.log4j.core.util.internal;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public final class SystemUtils {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static String getJavaVendor() {
        try {
            return System.getProperty("java.vendor");
        } catch (final SecurityException e) {
            LOGGER.warn("Unable to determine Java vendor.", e);
        }
        return "Unknown";
    }

    public static boolean isOsAndroid() {
        return getJavaVendor().contains("Android");
    }

    private SystemUtils() {}
}
