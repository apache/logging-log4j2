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

import com.lmax.disruptor.RingBuffer;
import javax.management.ObjectName;

/**
 * Instruments an LMAX Disruptor ring buffer.
 */
public class RingBufferAdmin implements RingBufferAdminMBean {

    private final RingBuffer<?> ringBuffer;
    private final ObjectName objectName;

    public static RingBufferAdmin forAsyncLogger(final RingBuffer<?> ringBuffer, final String contextName) {
        final String ctxName = Server.escape(contextName);
        final String name = String.format(PATTERN_ASYNC_LOGGER, ctxName);
        return new RingBufferAdmin(ringBuffer, name);
    }

    public static RingBufferAdmin forAsyncLoggerConfig(
            final RingBuffer<?> ringBuffer, final String contextName, final String configName) {
        final String ctxName = Server.escape(contextName);
        final String cfgName = Server.escape(configName);
        final String name = String.format(PATTERN_ASYNC_LOGGER_CONFIG, ctxName, cfgName);
        return new RingBufferAdmin(ringBuffer, name);
    }

    protected RingBufferAdmin(final RingBuffer<?> ringBuffer, final String mbeanName) {
        this.ringBuffer = ringBuffer;
        try {
            objectName = new ObjectName(mbeanName);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long getBufferSize() {
        return ringBuffer == null ? 0 : ringBuffer.getBufferSize();
    }

    @Override
    public long getRemainingCapacity() {
        return ringBuffer == null ? 0 : ringBuffer.remainingCapacity();
    }

    /**
     * Returns the {@code ObjectName} of this mbean.
     *
     * @return the {@code ObjectName}
     * @see RingBufferAdminMBean#PATTERN_ASYNC_LOGGER
     * @see RingBufferAdminMBean#PATTERN_ASYNC_LOGGER_CONFIG
     */
    public ObjectName getObjectName() {
        return objectName;
    }
}
