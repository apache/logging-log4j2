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
package org.apache.logging.log4j.core.net;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.logging.log4j.core.util.Closer;

/**
 * Finds free socket ports.
 */
public class FreePortFinder {

    /**
     * Finds free socket ports
     * 
     * @param count
     *        How many ports to find
     * @return An array of free port numbers.
     * @throws IOException
     *         if an I/O error occurs finding sockets
     */
    public static int[] findFreePorts(final int count) throws IOException {
        final int[] ports = new int[count];
        final ServerSocket[] sockets = new ServerSocket[count];
        try {
            for (int i = 0; i < count; ++i) {
                sockets[i] = new ServerSocket(0);
                ports[i] = sockets[i].getLocalPort();
            }
        } finally {
            for (int i = 0; i < count; ++i) {
                Closer.closeSilently(sockets[i]);
            }
        }
        return ports;
    }

}
