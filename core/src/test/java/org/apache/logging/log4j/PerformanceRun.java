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
package org.apache.logging.log4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Use this class to analyze Log4J-only performance.
 * <p/>
 * See {@linkplain PerformanceComparison} to compare performance with other logging frameworks.
 * 
 * @version $Id$
 */
public class PerformanceRun {

    private Logger logger = LogManager.getLogger(PerformanceRun.class.getName());

    // How many times should we try to log:
    private static final int COUNT = 1000000;

    private static final String CONFIG = "log4j2-perf.xml";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    }

    @Test
    public void testPerformance() throws Exception {
        System.out.println("Starting Log4j 2.0");
        long result3 = log4j2(COUNT);

        System.out.println("###############################################");
        System.out.println("Log4j 2.0: " + result3);
        System.out.println("###############################################");
    }

    // @Test
    public void testRawPerformance() throws Exception {
        OutputStream os = new FileOutputStream("target/testos.log", true);
        long result1 = writeToStream(COUNT, os);
        os.close();
        OutputStream bos = new BufferedOutputStream(new FileOutputStream("target/testbuffer.log", true));
        long result2 = writeToStream(COUNT, bos);
        bos.close();
        Writer w = new FileWriter("target/testwriter.log", true);
        long result3 = writeToWriter(COUNT, w);
        w.close();
        FileOutputStream cos = new FileOutputStream("target/testchannel.log", true);
        FileChannel channel = cos.getChannel();
        long result4 = writeToChannel(COUNT, channel);
        cos.close();
        System.out.println("###############################################");
        System.out.println("FileOutputStream: " + result1);
        System.out.println("BufferedOutputStream: " + result2);
        System.out.println("FileWriter: " + result3);
        System.out.println("FileChannel: " + result4);
        System.out.println("###############################################");
    }

    private long log4j2(int loop) {
        long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            if (logger.isDebugEnabled()) {
                logger.debug("SEE IF THIS IS LOGGED");
            }
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToWriter(int loop, Writer w) throws Exception {
        Integer j = new Integer(2);
        long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            w.write("SEE IF THIS IS LOGGED " + j + '.');
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToStream(int loop, OutputStream os) throws Exception {
        Integer j = new Integer(2);
        long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            os.write(getBytes("SEE IF THIS IS LOGGED " + j + '.'));
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToChannel(int loop, FileChannel channel) throws Exception {
        Integer j = new Integer(2);
        ByteBuffer buf = ByteBuffer.allocateDirect(8 * 1024);
        long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            channel.write(getByteBuffer(buf, "SEE IF THIS IS LOGGED " + j + '.'));
        }
        return (System.nanoTime() - start) / loop;
    }

    private ByteBuffer getByteBuffer(ByteBuffer buf, String s) {
        buf.clear();
        buf.put(s.getBytes());
        buf.flip();
        return buf;
    }

    private byte[] getBytes(String s) {
        return s.getBytes();
    }

}