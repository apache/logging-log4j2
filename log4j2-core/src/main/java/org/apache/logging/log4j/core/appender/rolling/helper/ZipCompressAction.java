/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.appender.rolling.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Compresses a file using Zip compression.
 */
public final class ZipCompressAction extends ActionBase {
    /**
     * Source file.
     */
    private final File source;

    /**
     * Destination file.
     */
    private final File destination;

    /**
     * If true, attempt to delete file on completion.
     */
    private final boolean deleteSource;


    /**
     * Create new instance of GZCompressAction.
     *
     * @param source       file to compress, may not be null.
     * @param destination  compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion.  Failure to delete
     *                     does not cause an exception to be thrown or affect return value.
     */
    public ZipCompressAction(final File source, final File destination, final boolean deleteSource) {
        if (source == null) {
            throw new NullPointerException("source");
        }

        if (destination == null) {
            throw new NullPointerException("destination");
        }

        this.source = source;
        this.destination = destination;
        this.deleteSource = deleteSource;
    }

    /**
     * Compress.
     *
     * @return true if successfully compressed.
     * @throws IOException on IO exception.
     */
    public boolean execute() throws IOException {
        return execute(source, destination, deleteSource);
    }

    /**
     * Compress a file.
     *
     * @param source       file to compress, may not be null.
     * @param destination  compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion.  Failure to delete
     *                     does not cause an exception to be thrown or affect return value.
     * @return true if source file compressed.
     * @throws IOException on IO exception.
     */
    public static boolean execute(final File source, final File destination, final boolean deleteSource)
        throws IOException {
        if (source.exists()) {
            FileInputStream fis = new FileInputStream(source);
            FileOutputStream fos = new FileOutputStream(destination);
            ZipOutputStream zos = new ZipOutputStream(fos);

            ZipEntry zipEntry = new ZipEntry(source.getName());
            zos.putNextEntry(zipEntry);

            byte[] inbuf = new byte[8102];
            int n;

            while ((n = fis.read(inbuf)) != -1) {
                zos.write(inbuf, 0, n);
            }

            zos.close();
            fis.close();

            if (deleteSource && !source.delete()) {
                logger.warn("Unable to delete " + source.toString() + ".");
            }

            return true;
        }

        return false;
    }

    /**
     * Capture exception.
     *
     * @param ex exception.
     */
    protected void reportException(final Exception ex) {
        logger.warn("Exception during compression of '" + source.toString() + "'.", ex);
    }
}
