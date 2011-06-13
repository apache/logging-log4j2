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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.internal.StatusLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 */
public class DatagramOutputStream extends OutputStream {

    private DatagramSocket ds;
    private InetAddress address;
    private int port;

    private byte[] data;

     /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger logger = StatusLogger.getLogger();

    public DatagramOutputStream(String host, int port) {
        this.port = port;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            String msg = "Could not find host " + host;
            logger.error(msg, ex);
            throw new AppenderRuntimeException(msg, ex);
        }

        try {
            ds = new DatagramSocket();
        } catch(SocketException ex) {
            String msg = "Could not instantiate DatagramSocket to " + host;
            logger.error(msg, ex);
            throw new AppenderRuntimeException(msg, ex);
        }
    }

    @Override
    public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
        copy(bytes, offset, length);
    }

    @Override
    public synchronized void write(int i) throws IOException {
        copy(new byte[] { (byte)(i >>> 24),(byte)(i >>> 16),(byte)(i >>> 8),(byte)i}, 0, 4);
    }

    @Override
    public synchronized void write(byte[] bytes) throws IOException {
        copy(bytes, 0, bytes.length);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (this.ds != null && this.address != null) {
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            ds.send(packet);
        }
        data = null;
    }

    @Override
    public synchronized void close() throws IOException {
        if (ds != null) {
            if (data != null) {
                flush();
            }
            ds.close();
            ds = null;
        }
    }

    private void copy(byte[] bytes, int offset, int length) {
        int index = data == null ? 0 : data.length;
        byte[] copy = new byte[length + index];
        if (data != null) {
            System.arraycopy(data, 0, copy, 0, index);
        }
        System.arraycopy(bytes, offset, copy, index, length);
        data = copy;
    }
}
