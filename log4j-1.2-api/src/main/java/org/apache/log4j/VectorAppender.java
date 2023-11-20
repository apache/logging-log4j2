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
package org.apache.log4j;

import java.util.Vector;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Appends logging events to a vector.
 */
public class VectorAppender extends AppenderSkeleton {

    public Vector vector;

    public VectorAppender() {
        vector = new Vector();
    }

    /**
     * Does nothing.
     */
    @Override
    public void activateOptions() {
        // noop
    }

    /**
     * This method is called by the {@link AppenderSkeleton#doAppend} method.
     *
     */
    @Override
    public void append(final LoggingEvent event) {
        // System.out.println("---Vector appender called with message ["+event.getRenderedMessage()+"].");
        // System.out.flush();
        try {
            Thread.sleep(100);
        } catch (final Exception e) {
            // ignore
        }
        vector.addElement(event);
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
    }

    public Vector getVector() {
        return vector;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
