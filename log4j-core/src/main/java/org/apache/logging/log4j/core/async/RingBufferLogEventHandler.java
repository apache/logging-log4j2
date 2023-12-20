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

import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.SequenceReportingEventHandler;

/**
 * This event handler gets passed messages from the RingBuffer as they become
 * available. Processing of these messages is done in a separate thread,
 * controlled by the {@code Executor} passed to the {@code Disruptor}
 * constructor.
 * <p>
 *     <strong>Warning:</strong> this class only works with Disruptor 3.x.
 * </p>
 * @deprecated Only used internally, will be removed in the next major version.
 */
@Deprecated
public class RingBufferLogEventHandler extends RingBufferLogEventHandler4
        implements SequenceReportingEventHandler<RingBufferLogEvent>, LifecycleAware {}
