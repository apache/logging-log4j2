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
package org.apache.logging.log4j.message;

/**
 * A Message that can render itself in more than one way. The format string is used by the
 * Message implementation as extra information that it may use to help it to determine how
 * to format itself. For example, MapMessage accepts a format of "XML" to tell it to render
 * the Map as XML instead of its default format of {key1="value1" key2="value2"}.
 */
public interface MultiformatMessage extends Message {

    /**
     * Returns the Message formatted as a String.
     *
     * @param formats An array of Strings that provide extra information about how to format the message.
     * Each MultiformatMessage implementation is free to use the provided formats however they choose.
     *
     * @return The message String.
     */
    String getFormattedMessage(String[] formats);

    /**
     * Returns the supported formats.
     * @return The supported formats.
     */
    String[] getFormats();
}
