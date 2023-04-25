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
package org.apache.logging.log4j.core.jmx;

/**
 * The MBean interface for monitoring and managing an LMAX Disruptor ring
 * buffer.
 */
public interface RingBufferAdminMBean {
    /**
     * ObjectName pattern ({@value}) for the RingBufferAdmin MBean that instruments
     * the global {@code AsyncLogger} ring buffer.
     * This pattern contains one variable: the name of the context.
     * <p>
     * You can find the registered RingBufferAdmin MBean for the global AsyncLogger like this:
     * </p>
     * <pre>
     * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
     * String pattern = String.format(RingBufferAdminMBean.PATTERN_ASYNC_LOGGER, &quot;*&quot;);
     * Set&lt;ObjectName&gt; asyncLoggerNames = mbs.queryNames(new ObjectName(pattern), null);
     * </pre>
     */
    String PATTERN_ASYNC_LOGGER = Server.DOMAIN + ":type=%s,component=AsyncLoggerRingBuffer";

    /**
     * ObjectName pattern ({@value}) for RingBufferAdmin MBeans that instrument
     * {@code AsyncLoggerConfig} ring buffers.
     * This pattern contains three variables, where the first is the name of the
     * context, the second and third are identical and the name of the instrumented logger config.
     * <p>
     * You can find all registered RingBufferAdmin MBeans like this:
     * </p>
     * <pre>
     * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
     * String pattern = String.format(RingBufferAdminMBean.PATTERN_ASYNC_LOGGER_CONFIG, &quot;*&quot;, &quot;*&quot;);
     * Set&lt;ObjectName&gt; asyncConfigNames = mbs.queryNames(new ObjectName(pattern), null);
     * </pre>
     */
    String PATTERN_ASYNC_LOGGER_CONFIG = Server.DOMAIN + ":type=%s,component=Loggers,name=%s,subtype=RingBuffer";

    /**
     * Returns the number of slots that the ring buffer was configured with.
     * Disruptor ring buffers are bounded-size data structures, this number does
     * not change during the life of the ring buffer.
     *
     * @return the number of slots that the ring buffer was configured with
     */
    long getBufferSize();

    /**
     * Returns the number of available slots in the ring buffer. May vary wildly
     * between invocations.
     *
     * @return the number of available slots in the ring buffer
     */
    long getRemainingCapacity();
}
