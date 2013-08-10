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
package org.apache.logging.log4j.core.util;

import java.lang.reflect.Field;

/**
 *
 */
public class Profiler {
    private static Object profiler;
    private static Class<?> profilingModes;
    private static Class<?> controllerClazz;

    static {
        try {
            controllerClazz = Class.forName("com.yourkit.api.Controller");
            profilingModes = Class.forName("com.yourkit.api.ProfilingModes");
            try {
                profiler = controllerClazz.newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
                System.out.println("Profiler was active, but failed due: " + e.getMessage());
            }
        }
        catch (final Exception e) {
            // Ignore
        }

    }

    public static boolean isActive() {
        return profiler != null;
    }

    public static void start() {

        if (profiler != null) {
            try {
                final Field f = profilingModes.getDeclaredField("CPU_SAMPLING");
                final Object[] args = new Object[2];
                args[0] = f.getLong(profilingModes);
                args[1] = "";
                final Class<?>[] parms = new Class<?>[] {long.class, String.class};
                controllerClazz.getMethod("startCPUProfiling", parms).invoke(profiler, args);
            }
            catch (final Exception e) {
                e.printStackTrace();
                System.out.println("Profiler was active, but failed due: " + e.getMessage());
            }
        }
    }

    public static void stop() {
        if (profiler != null) {
            try {
                final Field f = profilingModes.getDeclaredField("SNAPSHOT_WITHOUT_HEAP");
                final Object[] args = new Object[1];
                args[0] = f.getLong(profilingModes);
                final Class<?>[] parms = new Class<?>[] {long.class};
                profiler.getClass().getMethod("captureSnapshot", parms).invoke(profiler, args);
                profiler.getClass().getMethod("stopCPUProfiling").invoke(profiler);
            }
            catch (final Exception e) {
                e.printStackTrace();
                System.out.println("Profiler was active, but failed due: " + e.getMessage());
            }
        }
    }
}
