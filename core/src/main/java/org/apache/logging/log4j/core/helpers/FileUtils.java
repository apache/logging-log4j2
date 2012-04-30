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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

/**
 * File utilities.
 */
public final class FileUtils {

    /** Constant for the file URL protocol.*/
    private static final String PROTOCOL_FILE = "file";
    
    private static final String JBOSS_FILE = "vfsfile";

    private static Logger logger = StatusLogger.getLogger();

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
        if (uri == null || !uri.getScheme().equals(PROTOCOL_FILE) || !uri.getScheme().equals(JBOSS_FILE)) {
            return null;
        } else {
            try {
                return new File(URLDecoder.decode(uri.toURL().getFile(), "UTF8"));
            } catch (MalformedURLException ex) {
                logger.warn("Invalid URL " + uri, ex);
            } catch (UnsupportedEncodingException uee) {
                logger.warn("Invalid encoding: UTF8", uee);
            }
            return null;
        }
    }
    
    public static boolean isFile(URL url) {
        return url != null && (url.getProtocol().equals(PROTOCOL_FILE) || url.getProtocol().equals(JBOSS_FILE));
    }
}
