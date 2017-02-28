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

import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Use this class to analyze Log4j-only performance.
 * <p>
 * See {@linkplain PerformanceComparison} to compare performance with other logging frameworks.
 * </p>
 */
@Category(PerformanceTests.class)
public class PerformanceRun {

    private static final String CONFIG = "log4j2-perf.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private final Logger logger = context.getLogger(PerformanceRun.class.getName());

    // How many times should we try to log:
    private static final int COUNT = 1000000;

    @Test
    public void testPerformance() throws Exception {
        System.out.println("Starting Log4j 2.0");
        final long result3 = log4j2(COUNT);

        System.out.println("###############################################");
        System.out.println("Log4j 2.0: " + result3);
        System.out.println("###############################################");
    }

    @Test
    @Ignore("Why was this test disabled?")
    public void testRawPerformance() throws Exception {
        final OutputStream os = new FileOutputStream("target/testos.log", true);
        final long result1 = writeToStream(COUNT, os);
        os.close();
        final OutputStream bos = new BufferedOutputStream(new FileOutputStream("target/testbuffer.log", true));
        final long result2 = writeToStream(COUNT, bos);
        bos.close();
        final Writer w = new FileWriter("target/testwriter.log", true);
        final long result3 = writeToWriter(COUNT, w);
        w.close();
        final FileOutputStream cos = new FileOutputStream("target/testchannel.log", true);
        final FileChannel channel = cos.getChannel();
        final long result4 = writeToChannel(COUNT, channel);
        cos.close();
        System.out.println("###############################################");
        System.out.println("FileOutputStream: " + result1);
        System.out.println("BufferedOutputStream: " + result2);
        System.out.println("FileWriter: " + result3);
        System.out.println("FileChannel: " + result4);
        System.out.println("###############################################");
    }

    private long log4j2(final int loop) {
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            if (logger.isDebugEnabled()) {
                logger.debug("SEE IF THIS IS LOGGED");
            }
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToWriter(final int loop, final Writer w) throws Exception {
        final Integer j = Integer.valueOf(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            w.write("SEE IF THIS IS LOGGED " + j + '.');
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToStream(final int loop, final OutputStream os) throws Exception {
        final Integer j = Integer.valueOf(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            os.write(getBytes("SEE IF THIS IS LOGGED " + j + '.'));
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToChannel(final int loop, final FileChannel channel) throws Exception {
        final Integer j = Integer.valueOf(2);
        final ByteBuffer buf = ByteBuffer.allocateDirect(8 * 1024);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            channel.write(getByteBuffer(buf, "SEE IF THIS IS LOGGED " + j + '.'));
        }
        return (System.nanoTime() - start) / loop;
    }

    private ByteBuffer getByteBuffer(final ByteBuffer buf, final String s) {
        buf.clear();
        buf.put(s.getBytes());
        buf.flip();
        return buf;
    }

    private byte[] getBytes(final String s) {
        return s.getBytes();
    }

}
