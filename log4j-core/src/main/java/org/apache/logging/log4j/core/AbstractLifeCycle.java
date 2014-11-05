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

import java.io.Serializable;

import org.apache.logging.log4j.status.StatusLogger;

/**
 * A life cycle to be extended.
 * <p>
 * Wraps a {@link LifeCycle.State}.
 * </p>
 */
public class AbstractLifeCycle implements LifeCycle, Serializable {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    private static final long serialVersionUID = 1L;

    private volatile LifeCycle.State state = LifeCycle.State.INITIALIZED;

    protected boolean equalsImpl(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LifeCycle other = (LifeCycle) obj;
        if (state != other.getState()) {
            return false;
        }
        return true;
    }

    @Override
    public LifeCycle.State getState() {
        return this.state;
    }

    protected int hashCodeImpl() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    public boolean isInitialized() {
        return this.state == LifeCycle.State.INITIALIZED;
    }

    @Override
    public boolean isStarted() {
        return this.state == LifeCycle.State.STARTED;
    }

    public boolean isStarting() {
        return this.state == LifeCycle.State.STARTING;
    }

    @Override
    public boolean isStopped() {
        return this.state == LifeCycle.State.STOPPED;
    }

    public boolean isStopping() {
        return this.state == LifeCycle.State.STOPPING;
    }

    protected void setStarted() {
        this.setState(LifeCycle.State.STARTED);
    }

    protected void setStarting() {
        this.setState(LifeCycle.State.STARTING);
    }

    protected void setState(final LifeCycle.State newState) {
        this.state = newState;
        // Need a better string than this.toString() for the message
        // LOGGER.debug("{} {}", this.state, this);
    }

    protected void setStopped() {
        this.setState(LifeCycle.State.STOPPED);
    }

    protected void setStopping() {
        this.setState(LifeCycle.State.STOPPING);
    }

    @Override
    public void start() {
        this.setStarted();
    }

    @Override
    public void stop() {
        this.state = LifeCycle.State.STOPPED;
    }

}
