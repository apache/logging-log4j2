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

/**
* A Message that supports lazy initialization.
*/
public interface DeferredMessage {

  /**
  * Lazily perform expensive initialization tasks that would otherwise occur in
  * the constructor. For example, cloning mutable message content or gathering
  * data to be logged. Loggers are required to call this method immediately
  * after determining this Message is likely to be logged based on the log
  * Level and Marker, and before returning from the log method (i.e.
  * trace(message)). The Logger will not call initialize() if no logging will be
  * performed based on Level and Marker.
  *
  * Initialization must be performed before returning from the first call to this
  * method. Subsequent calls must be no-ops.
  *
  * @return This Message.
  */
 Message initialize();

}
