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
package org.apache.logging.log4j.core.appender.rolling;

/**
 * A <code>RollingPolicy</code> specifies the actions taken on a logging file rollover.
 *
 */
public interface RolloverStrategy {
    /**
     * Prepare for a rollover.  This method is called prior to closing the active log file, performs any necessary
     * preliminary actions and describes actions needed after close of current log file.
     *
     * @param manager The RollingFileManager name for current active log file.
     * @return Description of pending rollover, may be null to indicate no rollover at this time.
     * @throws SecurityException if denied access to log files.
     */
    RolloverDescription rollover(final RollingFileManager manager) throws SecurityException;
}
