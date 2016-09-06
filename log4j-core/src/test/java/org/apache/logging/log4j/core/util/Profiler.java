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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * YourKit Java Profiler helper class.
 */
public final class Profiler {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static Object profiler;
    private static Class<?> profilingModes;
    private static Class<?> controllerClazz;

    static {
        try {
            controllerClazz = LoaderUtil.loadClass("com.yourkit.api.Controller");
            profilingModes = LoaderUtil.loadClass("com.yourkit.api.ProfilingModes");
            try {
                profiler = controllerClazz.getConstructor().newInstance();
            } catch (final Exception e) {
                LOGGER.error("Profiler was active, but failed.", e);
            }
        }
        catch (final Exception ignored) {
            // Ignore
        }

    }

    private Profiler() {
    }

    public static boolean isActive() {
        return profiler != null;
    }

    private static long cpuSampling() throws NoSuchFieldException, IllegalAccessException {
        return profilingModes.getDeclaredField("CPU_SAMPLING").getLong(profilingModes);
    }

    private static long snapshotWithoutHeap() throws NoSuchFieldException, IllegalAccessException {
        return profilingModes.getDeclaredField("SNAPSHOT_WITHOUT_HEAP").getLong(profilingModes);
    }

    public static void start() {

        if (profiler != null) {
            try {
                controllerClazz
                        .getMethod("startCPUProfiling", long.class, String.class)
                        .invoke(profiler, cpuSampling(), Strings.EMPTY);
            }
            catch (final Exception e) {
                LOGGER.error("Profiler was active, but failed.", e);
            }
        }
    }

    public static void stop() {
        if (profiler != null) {
            try {
                controllerClazz
                        .getMethod("captureSnapshot", long.class)
                        .invoke(profiler, snapshotWithoutHeap());
                controllerClazz
                        .getMethod("stopCPUProfiling")
                        .invoke(profiler);
            }
            catch (final Exception e) {
                LOGGER.error("Profiler was active, but failed.", e);
            }
        }
    }
}
