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
package org.apache.logging.log4j.core.appender.rolling.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


/**
 * File rename action.
 */
public final class FileRenameAction extends ActionBase {
    /**
     * Source.
     */
    private final File source;

    /**
     * Destination.
     */
    private final File destination;

    /**
     * If true, rename empty files, otherwise delete empty files.
     */
    private final boolean renameEmptyFiles;

    /**
     * Creates an FileRenameAction.
     *
     * @param src              current file name.
     * @param dst              new file name.
     * @param renameEmptyFiles if true, rename file even if empty, otherwise delete empty files.
     */
    public FileRenameAction(final File src, final File dst, boolean renameEmptyFiles) {
        source = src;
        destination = dst;
        this.renameEmptyFiles = renameEmptyFiles;
    }

    /**
     * Rename file.
     *
     * @return true if successfully renamed.
     */
    public boolean execute() {
        return execute(source, destination, renameEmptyFiles);
    }

    /**
     * Rename file.
     *
     * @param source           current file name.
     * @param destination      new file name.
     * @param renameEmptyFiles if true, rename file even if empty, otherwise delete empty files.
     * @return true if successfully renamed.
     */
    public static boolean execute(final File source, final File destination, boolean renameEmptyFiles) {
        if (renameEmptyFiles || (source.length() > 0)) {
            try {

                boolean result = source.renameTo(destination);
                //System.out.println("Rename of " + source.getName() + " to " + destination.getName() + ": " + result);
                return result;
            } catch (Exception ex) {
                try {
                    copyFile(source, destination);
                    return source.delete();
                } catch (IOException iex) {
                    iex.printStackTrace();
                }
            }
        }

        return false;
    }

    private static void copyFile(final File source, final File destination) throws IOException {
        if (!destination.exists()) {
            destination.createNewFile();
        }

        FileChannel srcChannel = null;
        FileChannel destChannel = null;
        FileInputStream srcStream = null;
        FileOutputStream destStream = null;
        try {
            srcStream = new FileInputStream(source);
            destStream = new FileOutputStream(destination);
            srcChannel = srcStream.getChannel();
            destChannel = destStream.getChannel();
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            if (srcChannel != null) {
                srcChannel.close();
            }
            if (srcStream != null) {
                srcStream.close();
            }
            if (destChannel != null) {
                destChannel.close();
            }
            if (destStream != null) {
                destStream.close();
            }
        }
    }
}
