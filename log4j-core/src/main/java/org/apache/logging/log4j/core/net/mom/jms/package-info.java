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

/**
 * Supporting network code for JMS appenders.
 *
 * <p>Note that you can use JmsQueueReceiver or JmsTopicReceiver as executable main classes to receive log events over
 * JMS (sent via the appropriate JMS appender) that can be subsequently logged according to the configuration given to
 * the running process. Of course, use of these classes as standalone executables are entirely optional and can
 * be used directly in your application (e.g., through your Spring {@code beans.xml} configuration).</p>
 */
package org.apache.logging.log4j.core.net.mom.jms;
