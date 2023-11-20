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
package org.apache.logging.log4j.core.message;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.message.ThreadDumpMessage.ThreadInfoFactory;
import org.apache.logging.log4j.message.ThreadInformation;

/**
 * Factory to create extended thread information.
 */
@ServiceProvider(value = ThreadInfoFactory.class, resolution = Resolution.OPTIONAL)
public class ExtendedThreadInfoFactory implements ThreadInfoFactory {
    public ExtendedThreadInfoFactory() {
        final Method[] methods = ThreadInfo.class.getMethods();
        boolean basic = true;
        for (final Method method : methods) {
            if (method.getName().equals("getLockInfo")) {
                basic = false;
                break;
            }
        }
        if (basic) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Map<ThreadInformation, StackTraceElement[]> createThreadInfo() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] array = bean.dumpAllThreads(true, true);

        final Map<ThreadInformation, StackTraceElement[]> threads = new HashMap<>(array.length);
        for (final ThreadInfo info : array) {
            threads.put(new ExtendedThreadInformation(info), info.getStackTrace());
        }
        return threads;
    }
}
