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
package org.apache.logging.log4j.status;

import java.io.Closeable;
import java.util.EventListener;
import org.apache.logging.log4j.Level;

/**
 * Interface that allows implementers to be notified of events in the logging system.
 */
public interface StatusListener extends Closeable, EventListener {

    /**
     * Called as events occur to process the StatusData.
     * @param data The StatusData for the event.
     */
    void log(StatusData data);

    /**
     * Return the Log Level that this listener wants included.
     * @return the Log Level.
     */
    Level getStatusLevel();
}
