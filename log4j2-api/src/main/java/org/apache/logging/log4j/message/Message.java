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
package org.apache.logging.log4j.message;

import java.io.Serializable;
import java.util.Map;

/**
 * An interface for various Message implementations that can be logged.
 */
public interface Message extends Serializable {
    /**
     * Returns the Message formatted as a String.
     *
     * @return The message String.
     */
    String getFormattedMessage();

    /**
     * Returns the format portion of the Message
     *
     * @return
     */
    String getMessageFormat();

    /**
     * Returns parameter values, if any.
     *
     * @return An array of parameter values or null.
     */
    Object[] getParameters();


    /**
     * Returns a Map of data that the Message would like to aid in formatting. The
     * Message will construct the map with the keys it is requesting the implementation to
     * provide values for. The Message must be able to return a formatted message even if
     * no hints are provided.
     * @return The Message hints.
     */
    Map<MessageHint, String> getHints();
}
