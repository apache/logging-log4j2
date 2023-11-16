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
package org.apache.logging.log4j.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Provides the PID of the current JVM.
 *
 * @since 2.9
 */
public class ProcessIdUtil {

    public static final String DEFAULT_PROCESSID = "-";

    public static String getProcessId() {
        try {
            // LOG4J2-2126 use reflection to improve compatibility with Android Platform which does not support JMX
            // extensions
            final Class<?> managementFactoryClass = Class.forName("java.lang.management.ManagementFactory");
            final Method getRuntimeMXBean = managementFactoryClass.getDeclaredMethod("getRuntimeMXBean");
            final Class<?> runtimeMXBeanClass = Class.forName("java.lang.management.RuntimeMXBean");
            final Method getName = runtimeMXBeanClass.getDeclaredMethod("getName");

            final Object runtimeMXBean = getRuntimeMXBean.invoke(null);
            final String name = (String) getName.invoke(runtimeMXBean);
            // Split into first@rest
            return name.split("@", 2)[0]; // likely works on most platforms
        } catch (final Exception ex) {
            try {
                return new File("/proc/self").getCanonicalFile().getName(); // try a Linux-specific way
            } catch (final IOException ignoredUseDefault) {
                // Ignore exception.
            }
        }
        return DEFAULT_PROCESSID;
    }
}
