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

package org.apache.logging.log4j.core;

import java.io.File;

import org.apache.logging.log4j.LogManager;

public class CoreLoggerContexts {

    private static void sleepAndCheck(final File checkFilePresence) throws InterruptedException {
        Thread.sleep(100);
        if (checkFilePresence.length() == 0) {
            Thread.sleep(500);
        }
    }

    public static void stopLoggerContext() {
        ((LifeCycle) LogManager.getContext()).stop(); // stops async thread
    }

    public static void stopLoggerContext(final boolean currentContext) {
        ((LifeCycle) LogManager.getContext(currentContext)).stop(); // stops async thread
    }

    public static void stopLoggerContext(final boolean currentContext, final File checkFilePresence) throws InterruptedException {
        stopLoggerContext(currentContext);
        sleepAndCheck(checkFilePresence);
    }

    public static void stopLoggerContext(final File checkFilePresence) throws InterruptedException {
        stopLoggerContext();
        sleepAndCheck(checkFilePresence);
    }

}
