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
package org.apache.logging.log4j.spring.boot;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.internal.LogManagerStatus;
import org.springframework.core.env.Environment;

/**
 * Provides access to the Spring Environment.
 */
public class SpringEnvironmentHolder {

    private volatile Environment environment;
    private Lock lock = new ReentrantLock();


    protected Environment getEnvironment() {
        if (environment == null && LogManagerStatus.isInitialized() && LogManager.getFactory() != null &&
                LogManager.getFactory().hasContext(SpringEnvironmentHolder.class.getName(), null, false)) {
            lock.lock();
            try {
                if (environment == null) {
                    Object obj = LogManager.getContext(false).getObject(Log4j2CloudConfigLoggingSystem.ENVIRONMENT_KEY);
                    environment = obj instanceof Environment ? (Environment) obj : null;
                }
            } finally {
                lock.unlock();
            }
        }
        return environment;
    }
}
