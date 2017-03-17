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

package org.apache.logging.log4j.core.net.mom.jms;

/**
 * Receives Topic messages that contain LogEvents. This implementation expects that all messages
 * are serialized log events.
 */
public class JmsTopicReceiver extends AbstractJmsReceiver {

    private JmsTopicReceiver() {
    }

    /**
     * Main startup for the receiver.
     *
     * @param args The command line arguments.
     * @throws Exception if an error occurs.
     */
    public static void main(final String[] args) throws Exception {
        final JmsTopicReceiver receiver = new JmsTopicReceiver();
        receiver.doMain(args);
    }

    @Override
    protected void usage() {
        System.err.println("Wrong number of arguments.");
        System.err.println("Usage: java " + JmsTopicReceiver.class.getName()
            + " TopicConnectionFactoryBindingName TopicBindingName username password");
    }
}
