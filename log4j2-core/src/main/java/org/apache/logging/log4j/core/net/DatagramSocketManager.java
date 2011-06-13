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

import org.apache.logging.log4j.core.appender.ManagerFactory;

import java.io.OutputStream;

/**
 *
 */
public class DatagramSocketManager extends AbstractSocketManager {

    private static ManagerFactory factory = new DatagramSocketManagerFactory();

    public static DatagramSocketManager getSocketManager(String host, int port) {
        if (host == null && host.length() == 0) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("A port value is required");
        }
        return (DatagramSocketManager) getManager("UDP:" + host +":" + port, factory,
            new FactoryData(host, port));
    }

    public DatagramSocketManager(OutputStream os, String name, String host, int port) {
        super(name, os, null, host, port);
    }


    private static class FactoryData {
        String host;
        int port;

        public FactoryData(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    private static class DatagramSocketManagerFactory implements ManagerFactory<DatagramSocketManager, FactoryData> {

        public DatagramSocketManager createManager(String name, FactoryData data) {
            OutputStream os = new DatagramOutputStream(data.host, data.port);
            return new DatagramSocketManager(os, name, data.host, data.port);
        }
    }
}
