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

package org.apache.logging.log4j.plugins.di;

import java.util.Comparator;

/**
 * Provides a ServiceLoader class for configuring an {@link Injector} when initialized. When {@link Injector#init()} is invoked,
 * all service instances are sorted by {@link #getOrder()} in the natural integer order (from negative to positive), and each
 * service is provided an instance of the Injector via {@link #configure(Injector)}. This extension is provided for lower level
 * control of default bindings, overriding bindings, and otherwise examining the state of an Injector.
 */
public interface InjectorCallback {

    Comparator<InjectorCallback> COMPARATOR = Comparator.comparingInt(InjectorCallback::getOrder)
            .thenComparing(instance -> instance.getClass().getName());

    /**
     * Configures the provided Injector. Service providers must implement this method to perform any callback logic on the
     * initializing Injector.
     */
    void configure(final Injector injector);

    /**
     * Returns the order of this callback. Callbacks are invoked in {@link Integer#compare(int, int)} order.
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Returns a human-readable description of this callback.
     */
    String toString();
}
