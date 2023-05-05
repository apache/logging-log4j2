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
package org.apache.logging.log4j.core.net;

/**
 * Enumerates the <a href="https://tools.ietf.org/html/rfc1349">RFC 1349</a> TOS field.
 *
 * <ul>
 * <li><code>IPTOS_LOWCOST (0x02)</code></li>
 * <li><code>IPTOS_RELIABILITY (0x04)</code></li>
 * <li><code>IPTOS_THROUGHPUT (0x08)</code></li>
 * <li><code>IPTOS_LOWDELAY (0x10)</code></li>
 * <ul>
 */
public enum Rfc1349TrafficClass {

    /**
     * IPTOS_NORMAL (0x00)
     */
    IPTOS_NORMAL(0x00),

    /**
     * IPTOS_LOWCOST (0x02)
     */
    IPTOS_LOWCOST(0x02),

    /**
     * IPTOS_LOWDELAY (0x10)
     */
    IPTOS_LOWDELAY(0x10),

    /**
     * IPTOS_RELIABILITY (0x04)
     */
    IPTOS_RELIABILITY(0x04),

    /**
     * IPTOS_THROUGHPUT (0x08)
     */
    IPTOS_THROUGHPUT(0x08);

    private final int trafficClass;

    private Rfc1349TrafficClass(final int trafficClass) {
        this.trafficClass = trafficClass;
    }

    /**
     * Gets the value.
     *
     * @return the value.
     */
    public int value() {
        return trafficClass;
    }
}
