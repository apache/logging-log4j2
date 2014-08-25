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

import java.io.Serializable;

/**
 * ShutdownRegistrationStrategy that simply uses {@link Runtime#addShutdownHook(Thread)}. If no strategy is specified,
 * this one is used for shutdown hook registration.
 *
 * @since 2.1
 */
public class DefaultShutdownRegistrationStrategy implements ShutdownRegistrationStrategy, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public void registerShutdownHook(final Thread hook) {
        Runtime.getRuntime().addShutdownHook(hook);
    }

    @Override
    public void unregisterShutdownHook(final Thread hook) {
        Runtime.getRuntime().removeShutdownHook(hook);
    }
}
