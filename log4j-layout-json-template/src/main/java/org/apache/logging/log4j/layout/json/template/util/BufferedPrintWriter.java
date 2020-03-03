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
package org.apache.logging.log4j.layout.json.template.util;

import java.io.PrintWriter;

public final class BufferedPrintWriter extends PrintWriter {

    private final BufferedWriter bufferedWriter;

    private BufferedPrintWriter(final BufferedWriter bufferedWriter) {
        super(bufferedWriter, false);
        this.bufferedWriter = bufferedWriter;
    }

    public static BufferedPrintWriter ofCapacity(final int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException(
                    "was expecting a non-negative capacity: " + capacity);
        }
        final BufferedWriter bufferedWriter = new BufferedWriter(capacity);
        return new BufferedPrintWriter(bufferedWriter);
    }

    public char[] getBuffer() {
        return bufferedWriter.getBuffer();
    }

    public int getPosition() {
        return bufferedWriter.getPosition();
    }

    public int getCapacity() {
        return bufferedWriter.getCapacity();
    }

    @Override
    public void close() {
        bufferedWriter.close();
    }

}
