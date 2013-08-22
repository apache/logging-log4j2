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
package org.apache.logging.log4j.core.helpers;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * File utilities.
 */
public final class FileUtils {

    /** Constant for the file URL protocol.*/
    private static final String PROTOCOL_FILE = "file";

    private static final String JBOSS_FILE = "vfsfile";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private FileUtils() {
    }

      /**
     * Tries to convert the specified URL to a file object. If this fails,
     * <b>null</b> is returned.
     *
     * @param uri the URI
     * @return the resulting file object
     */
    public static File fileFromURI(URI uri) {
        if (uri == null || (uri.getScheme() != null &&
            (!PROTOCOL_FILE.equals(uri.getScheme()) && !JBOSS_FILE.equals(uri.getScheme())))) {
            return null;
        }
        if (uri.getScheme() == null) {
            try {
                uri = new File(uri.getPath()).toURI();
            } catch (final Exception ex) {
                LOGGER.warn("Invalid URI " + uri);
                return null;
            }
        }
        try {
            return new File(URLDecoder.decode(uri.toURL().getFile(), "UTF8"));
        } catch (final MalformedURLException ex) {
            LOGGER.warn("Invalid URL " + uri, ex);
        } catch (final UnsupportedEncodingException uee) {
            LOGGER.warn("Invalid encoding: UTF8", uee);
        }
        return null;
    }

    public static boolean isFile(final URL url) {
        return url != null && (url.getProtocol().equals(PROTOCOL_FILE) || url.getProtocol().equals(JBOSS_FILE));
    }

    /**
     * Asserts that the given directory exists and creates it if necessary.
     * @param dir the directory that shall exist
     * @param createDirectoryIfNotExisting specifies if the directory shall be created if it does not exist.
     * @throws java.io.IOException thrown if the directory could not be created.
     */
    public static void mkdir(final File dir, final boolean createDirectoryIfNotExisting ) throws IOException {
        // commons io FileUtils.forceMkdir would be useful here, we just want to omit this dependency
        if (!dir.exists()) {
            if(!createDirectoryIfNotExisting) {
                throw new IOException( "The directory " + dir.getAbsolutePath() + " does not exist." );
            }
            if(!dir.mkdirs()) {
                throw new IOException( "Could not create directory " + dir.getAbsolutePath() );
            }
        }
        if (!dir.isDirectory()) {
            throw new IOException("File " + dir + " exists and is not a directory. Unable to create directory.");
        }
    }
}
