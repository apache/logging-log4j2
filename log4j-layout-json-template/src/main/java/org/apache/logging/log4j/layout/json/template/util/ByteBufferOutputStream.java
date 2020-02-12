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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ByteBufferOutputStream extends OutputStream {

    private final ByteBuffer byteBuffer;

    public ByteBufferOutputStream(final int byteCount) {
        this.byteBuffer = ByteBuffer.allocate(byteCount);
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public void write(final int codeInt) {
        byte codeByte = (byte) codeInt;
        byteBuffer.put(codeByte);
    }

    @Override
    public void write(final byte[] buffer) {
        byteBuffer.put(buffer);
    }

    @Override
    public void write(final byte[] buffer, final int offset, final int length) {
        byteBuffer.put(buffer, offset, length);
    }

    public byte[] toByteArray() {
        final int size = byteBuffer.position();
        final byte[] buffer = new byte[size];
        System.arraycopy(byteBuffer.array(), 0, buffer, 0, size);
        return buffer;
    }

    public String toString(final Charset charset) {
        return new String(byteBuffer.array(), 0, byteBuffer.position(), charset);
    }

}
