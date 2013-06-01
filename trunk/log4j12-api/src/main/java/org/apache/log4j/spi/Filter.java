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
package org.apache.log4j.spi;

/**
 * @since 0.9.0
 */
public abstract class Filter {

    /**
     * The log event must be dropped immediately without consulting
     * with the remaining filters, if any, in the chain.
     */
    public static final int DENY = -1;

    /**
     * This filter is neutral with respect to the log event. The
     * remaining filters, if any, should be consulted for a final decision.
     */
    public static final int NEUTRAL = 0;

    /**
     * The log event must be logged immediately without consulting with
     * the remaining filters, if any, in the chain.
     */
    public static final int ACCEPT = 1;

    /**
     * Points to the next filter in the filter chain.
     *
     * @deprecated As of 1.2.12, use {@link #getNext} and {@link #setNext} instead
     */
    @Deprecated
    public Filter next;

    /**
     * Usually filters options become active when set. We provide a
     * default do-nothing implementation for convenience.
     */
    public void activateOptions() {
    }


    /**
     * <p>If the decision is <code>DENY</code>, then the event will be
     * dropped. If the decision is <code>NEUTRAL</code>, then the next
     * filter, if any, will be invoked. If the decision is ACCEPT then
     * the event will be logged without consulting with other filters in
     * the chain.
     *
     * @param event The LoggingEvent to decide upon.
     * @return decision The decision of the filter.
     */
    public abstract int decide(LoggingEvent event);

    /**
     * Set the next filter pointer.
     * @param next The next Filter.
     */
    public void setNext(final Filter next) {
        this.next = next;
    }

    /**
     * Return the pointer to the next filter.
     * @return The next Filter.
     */
    public Filter getNext() {
        return next;
    }

}
