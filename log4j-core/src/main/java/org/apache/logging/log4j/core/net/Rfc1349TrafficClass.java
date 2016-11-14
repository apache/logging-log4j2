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

package org.apache.logging.log4j.core.net;

/**
 * Enumerates the RFC 1349 TOS field.
 * 
 * <ul>
 * <li>1000 -- minimize delay</li>
 * <li>0100 -- maximize throughput</li>
 * <li>0010 -- maximize reliability</li>
 * <li>0001 -- minimize monetary cost</li>
 * <li>0000 -- normal service</li>
 * <ul>
 */
public enum Rfc1349TrafficClass {

    // @formatter:off
    IPTOS_NORMAL(0x00),
    IPTOS_LOWCOST(0x02),
    IPTOS_LOWDELAY (0x10),
    IPTOS_RELIABILITY (0x04),
    IPTOS_THROUGHPUT (0x08);
    // @formatter:on

    private final int trafficClass;

    private Rfc1349TrafficClass(final int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public int value() {
        return trafficClass;
    }
}
