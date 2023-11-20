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
package org.apache.log4j.or;

import org.apache.log4j.Layout;

/**
 */
public class ThreadGroupRenderer implements ObjectRenderer {

    @Override
    public String doRender(final Object obj) {
        if (obj instanceof ThreadGroup) {
            final StringBuilder sb = new StringBuilder();
            final ThreadGroup threadGroup = (ThreadGroup) obj;
            sb.append("java.lang.ThreadGroup[name=");
            sb.append(threadGroup.getName());
            sb.append(", maxpri=");
            sb.append(threadGroup.getMaxPriority());
            sb.append("]");
            final Thread[] threads = new Thread[threadGroup.activeCount()];
            threadGroup.enumerate(threads);
            for (Thread thread : threads) {
                sb.append(Layout.LINE_SEP);
                sb.append("   Thread=[");
                sb.append(thread.getName());
                sb.append(",");
                sb.append(thread.getPriority());
                sb.append(",");
                sb.append(thread.isDaemon());
                sb.append("]");
            }
            return sb.toString();
        }
        try {
            // this is the best we can do
            return obj.toString();
        } catch (Exception ex) {
            return ex.toString();
        }
    }
}
