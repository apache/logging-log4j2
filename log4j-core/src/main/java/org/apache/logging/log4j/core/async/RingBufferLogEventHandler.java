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
package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;

/**
 * This event handler gets passed messages from the RingBuffer as they become
 * available. Processing of these messages is done in a separate thread,
 * controlled by the {@code Executor} passed to the {@code Disruptor}
 * constructor.
 */
public class RingBufferLogEventHandler implements
        SequenceReportingEventHandler<RingBufferLogEvent>, LifecycleAware {

    private static final int NOTIFY_PROGRESS_THRESHOLD = 50;
    private Sequence sequenceCallback;
    private int counter;
    private long threadId = -1;

    @Override
    public void setSequenceCallback(final Sequence sequenceCallback) {
        this.sequenceCallback = sequenceCallback;
    }

    @Override
    public void onEvent(final RingBufferLogEvent event, final long sequence,
            final boolean endOfBatch) throws Exception {
        event.execute(endOfBatch);
        event.clear();

        // notify the BatchEventProcessor that the sequence has progressed.
        // Without this callback the sequence would not be progressed
        // until the batch has completely finished.
        if (++counter > NOTIFY_PROGRESS_THRESHOLD) {
            sequenceCallback.set(sequence);
            counter = 0;
        }
    }

    /**
     * Returns the thread ID of the background consumer thread, or {@code -1} if the background thread has not started
     * yet.
     * @return the thread ID of the background consumer thread, or {@code -1}
     */
    public long getThreadId() {
        return threadId;
    }

    @Override
    public void onStart() {
        threadId = Thread.currentThread().getId();
    }

    @Override
    public void onShutdown() {
    }
}
