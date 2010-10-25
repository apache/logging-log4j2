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
 * An interface for various Message implementations that can be logged. Messages can act as wrappers
 * around Objects so that user can have control over converting Objects to Strings when necessary without
 * requiring complicated formatters and as a way to manipulate the message based on information available
 * at runtime such as the locale of the system.
 * @doubt Interfaces should rarely extend Serializable according to Effective Java 2nd Ed pg 291.
 * (RG) That section also says "If a class or interface exists primarily to participate in a framework that
 * requires all participants to implement Serializable, then it makes perfect sense for the class or
 * interface to implement or extend Serializable". Such is the case here as the LogEvent must be Serializable.
 */
public interface Message extends Serializable {
    /**
     * Returns the Message formatted as a String.
     *
     * @return The message String.
     */
    String getFormattedMessage();

    /**
     * Returns the format portion of the Message.
     *
     * @return The message format.
     * @doubt Do all messages have a format?  What syntax?  Using a Formatter object could be cleaner.
     * (RG) In SimpleMessage the format is identical to the formatted message. In ParameterizedMessage and
     * StructuredDataMessage itis not. It is up to the Message implementer to determine what this
     * method will return. A Formatter is inappropriate as this is very specific to the Message
     * implementation so it isn't clear to me how having a Formatter separate from the Message would be cleaner.
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
     * @doubt would seem to go better into a formatter or format object. (RG) A Formatter would have
     * to understand every type of object that could be passed to it or you would have to
     * configure an endless number of formatters on loggers and somehow pick the correct one. A Message
     * implementation formats based only on what can be placed into the Message and what hints are provided.
     */
    Map<MessageHint, String> getHints();
}
