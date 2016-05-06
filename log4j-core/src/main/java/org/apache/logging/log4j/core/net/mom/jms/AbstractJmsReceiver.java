/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.core.net.mom.jms;

import org.apache.logging.log4j.core.net.server.JmsServer;

/**
 * Common JMS server functionality.
 *
 * @since 2.6
 */
public abstract class AbstractJmsReceiver {

    /**
     * Prints out usage information to {@linkplain System#err standard error}.
     */
    protected abstract void usage();

    /**
     * Executes a JmsServer with the given command line arguments.
     *
     * @param args command line arguments
     * @throws Exception
     */
    protected void doMain(final String... args) throws Exception {
        if (args.length != 4) {
            usage();
            System.exit(1);
        }
        final JmsServer server = new JmsServer(args[0], args[1], args[2], args[3]);
        server.run();
    }
}
