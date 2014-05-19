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

/**
 * A life cycle to be extended.
 */
public class AbstractLifeCycle implements LifeCycle {

    protected volatile LifeCycle.State state = LifeCycle.State.INITIALIZED;

    public LifeCycle.State getState() {
        return this.state;
    }
    
    @Override
    public void start() {
        this.state = LifeCycle.State.STARTED;
    }

    @Override
    public void stop() {
        this.state = LifeCycle.State.STOPPED;
    }

    @Override
    public boolean isStarted() {
        return this.state == LifeCycle.State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return this.state == LifeCycle.State.STOPPED;
    }

}
