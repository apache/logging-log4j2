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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class AsyncLoggerClassLoadDeadlock {
    static {
        final Logger log = LogManager.getLogger("com.foo.bar.deadlock");
        final Exception e = new Exception();
        // the key to reproducing the problem is to fill up the ring buffer so that
        // log.info call will block on ring buffer as well
        for (int i = 0; i < AsyncLoggerClassLoadDeadlockTest.RING_BUFFER_SIZE * 2; ++i) {
            log.info("clinit", e);
        }
    }
}
