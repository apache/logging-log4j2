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
package org.apache.logging.log4j.jackson.json.template.layout.util;

import java.io.Writer;

public final class BufferedWriter extends Writer {

    private final char[] buffer;

    private int position;

    BufferedWriter(final int capacity) {
        this.buffer = new char[capacity];
        this.position = 0;
    }

    char[] getBuffer() {
        return buffer;
    }

    int getPosition() {
        return position;
    }

    int getCapacity() {
        return buffer.length;
    }

    @Override
    public void write(final char[] source, final int offset, final int length) {
        System.arraycopy(source, offset, buffer, position, length);
        position += length;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        position = 0;
    }

}
