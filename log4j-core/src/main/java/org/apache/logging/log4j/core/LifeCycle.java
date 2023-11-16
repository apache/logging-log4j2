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
package org.apache.logging.log4j.core;

/**
 * All proper Java frameworks implement some sort of object life cycle. In Log4j, the main interface for handling
 * the life cycle context of an object is this one. An object first starts in the {@link State#INITIALIZED} state
 * by default to indicate the class has been loaded. From here, calling the {@link #start()} method will change this
 * state to {@link State#STARTING}. After successfully being started, this state is changed to {@link State#STARTED}.
 * When the {@link #stop()} is called, this goes into the {@link State#STOPPING} state. After successfully being
 * stopped, this goes into the {@link State#STOPPED} state. In most circumstances, implementation classes should
 * store their {@link State} in a {@code volatile} field or inside an
 * {@link java.util.concurrent.atomic.AtomicReference} dependent on synchronization and concurrency requirements.
 *
 * @see AbstractLifeCycle
 */
public interface LifeCycle {

    /**
     * Status of a life cycle like a {@link LoggerContext}.
     */
    enum State {
        /** Object is in its initial state and not yet initialized. */
        INITIALIZING,
        /** Initialized but not yet started. */
        INITIALIZED,
        /** In the process of starting. */
        STARTING,
        /** Has started. */
        STARTED,
        /** Stopping is in progress. */
        STOPPING,
        /** Has stopped. */
        STOPPED
    }

    /**
     * Gets the life-cycle state.
     *
     * @return the life-cycle state
     */
    State getState();

    void initialize();

    void start();

    void stop();

    boolean isStarted();

    boolean isStopped();
}
