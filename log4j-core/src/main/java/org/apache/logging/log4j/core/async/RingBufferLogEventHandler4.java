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
package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;

/**
 * This event handler gets passed messages from the RingBuffer as they become
 * available. Processing of these messages is done in a separate thread,
 * controlled by the {@code Executor} passed to the {@code Disruptor}
 * constructor.
 *  * <p>
 *  *     <strong>Warning:</strong> this class only works with Disruptor 4.x.
 *  * </p>
 */
class RingBufferLogEventHandler4 implements EventHandler<RingBufferLogEvent> {

    private static final int NOTIFY_PROGRESS_THRESHOLD = 50;
    private Sequence sequenceCallback;
    private int counter;
    private long threadId = -1;

    /*
     * Overrides a method from Disruptor 4.x. Do not remove.
     */
    public void setSequenceCallback(final Sequence sequenceCallback) {
        this.sequenceCallback = sequenceCallback;
    }

    @Override
    public void onEvent(final RingBufferLogEvent event, final long sequence, final boolean endOfBatch)
            throws Exception {
        try {
            // RingBufferLogEvents are populated by an EventTranslator. If an exception is thrown during event
            // translation, the event may not be fully populated, but Disruptor requires that the associated sequence
            // still be published since a slot has already been claimed in the ring buffer. Ignore any such unpopulated
            // events. The exception that occurred during translation will have already been propagated.
            if (event.isPopulated()) {
                event.execute(endOfBatch);
            }
        } finally {
            event.clear();
            // notify the BatchEventProcessor that the sequence has progressed.
            // Without this callback the sequence would not be progressed
            // until the batch has completely finished.
            notifyCallback(sequence);
        }
    }

    private void notifyCallback(final long sequence) {
        if (++counter > NOTIFY_PROGRESS_THRESHOLD) {
            sequenceCallback.set(sequence);
            counter = 0;
        }
    }

    /**
     * Returns the thread ID of the background consumer thread, or {@code -1} if the background thread has not started
     * yet.
     *
     * @return the thread ID of the background consumer thread, or {@code -1}
     */
    public long getThreadId() {
        return threadId;
    }

    /*
     * Overrides a method from Disruptor 4.x. Do not remove.
     */
    public void onStart() {
        threadId = Thread.currentThread().getId();
    }

    /*
     * Overrides a method from Disruptor 4.x. Do not remove.
     */
    public void onShutdown() {}
}
