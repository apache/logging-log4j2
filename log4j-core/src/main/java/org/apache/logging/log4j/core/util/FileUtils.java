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
package org.apache.logging.log4j.core.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * File utilities.
 */
public final class FileUtils {

    /** Constant for the file URL protocol. */
    private static final String PROTOCOL_FILE = "file";

    private static final String JBOSS_FILE = "vfsfile";

    private static final long FILE_CREATION_TIME_MS_MAX_DELTA = 1250L;

    private static final Logger LOGGER = StatusLogger.getLogger();

    private FileUtils() {
    }

    /**
     * Tries to convert the specified URI to a file object. If this fails, <b>null</b> is returned.
     *
     * @param uri the URI
     * @return the resulting file object
     */
    public static File fileFromUri(URI uri) {
        // There MUST be a better way to do this. TODO Search other ASL projects...
        if (uri == null || (uri.getScheme() != null
                && (!PROTOCOL_FILE.equals(uri.getScheme()) && !JBOSS_FILE.equals(uri.getScheme())))) {
            return null;
        }
        if (uri.getScheme() == null) {
            File file = new File(uri.toString());
            if (file.exists()) {
                return file;
            }
            try {
                final String path = uri.getPath();
                file = new File(path);
                if (file.exists()) {
                    return file;
                }
                uri = new File(path).toURI();
            } catch (final Exception ex) {
                LOGGER.warn("Invalid URI {}", uri);
                return null;
            }
        }
        final String charsetName = StandardCharsets.UTF_8.name();
        try {
            String fileName = uri.toURL().getFile();
            if (new File(fileName).exists()) { // LOG4J2-466
                return new File(fileName); // allow files with '+' char in name
            }
            fileName = URLDecoder.decode(fileName, charsetName);
            return new File(fileName);
        } catch (final MalformedURLException ex) {
            LOGGER.warn("Invalid URL {}", uri, ex);
        } catch (final UnsupportedEncodingException uee) {
            LOGGER.warn("Invalid encoding: {}", charsetName, uee);
        }
        return null;
    }

    public static boolean isFile(final URL url) {
        return url != null && (url.getProtocol().equals(PROTOCOL_FILE) || url.getProtocol().equals(JBOSS_FILE));
    }

    public static String getFileExtension(final File file) {
        final String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }

    /**
     * Asserts that the given directory exists and creates it if necessary.
     *
     * @param dir the directory that shall exist
     * @param createDirectoryIfNotExisting specifies if the directory shall be created if it does not exist.
     * @throws java.io.IOException thrown if the directory could not be created.
     */
    public static void mkdir(final File dir, final boolean createDirectoryIfNotExisting) throws IOException {
        // commons io FileUtils.forceMkdir would be useful here, we just want to omit this dependency
        if (!dir.exists()) {
            if (!createDirectoryIfNotExisting) {
                throw new IOException("The directory " + dir.getAbsolutePath() + " does not exist.");
            }
            if (!dir.mkdirs()) {
                throw new IOException("Could not create directory " + dir.getAbsolutePath());
            }
        }
        if (!dir.isDirectory()) {
            throw new IOException("File " + dir + " exists and is not a directory. Unable to create directory.");
        }
    }

    /**
     * Creates the parent directories for the given File.
     *
     * @param file
     * @throws IOException
     */
    public static void makeParentDirs(final File file) throws IOException {
        final File parent = Objects.requireNonNull(file, "file").getCanonicalFile().getParentFile();
        if (parent != null) {
            mkdir(parent, true);
        }
    }

    /**
     * Define file posix attribute view on a path/file.
     *
     * @param path Target path
     * @param filePermissions Permissions to apply
     * @param fileOwner File owner
     * @param fileGroup File group
     * @throws IOException If IO error during definition of file attribute view
     */
    public static void defineFilePosixAttributeView(final Path path,
            final Set<PosixFilePermission> filePermissions,
            final String fileOwner,
            final String fileGroup) throws IOException {
        final PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (view != null) {
            final UserPrincipalLookupService lookupService = FileSystems.getDefault()
                    .getUserPrincipalLookupService();
            if (fileOwner != null) {
                final UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(fileOwner);
                if (userPrincipal != null) {
                    // If not sudoers member, it will throw Operation not permitted
                    // Only processes with an effective user ID equal to the user ID
                    // of the file or with appropriate privileges may change the ownership of a file.
                    // If _POSIX_CHOWN_RESTRICTED is in effect for path
                    view.setOwner(userPrincipal);
                }
            }
            if (fileGroup != null) {
                final GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(fileGroup);
                if (groupPrincipal != null) {
                    // The current user id should be members of this group,
                    // if not will raise Operation not permitted
                    view.setGroup(groupPrincipal);
                }
            }
            if (filePermissions != null) {
                view.setPermissions(filePermissions);
            }
        }
    }

    /**
     * Check if posix file attribute view is supported on the default FileSystem.
     *
     * @return true if posix file attribute view supported, false otherwise
     */
    public static boolean isFilePosixAttributeViewSupported() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    /**
     * Update file creation time attribute to match the provided creationTimeAsMS, but only if the
     *   file creationTime attribute read from the path (file) is nonzero (0L indicates creationTime is not
     *   supported by the underlying OS).  In addition, if the existing creation time differs by less than
     *   FILE_CREATION_TIME_MS_MAX_DELTA, the method will <b>not</b> attempt a change (already close enough).
     * 
     * Note: May be used to compensate for Windows "file system tunneling", where a new file's creationTime
     *   attribute matches that of an old moved/deleted file of the same name (instead of the expected
     *   current system time as the creationTime).  See the blog at the site below for further details:
     *   <a href="https://devblogs.microsoft.com/oldnewthing/20050715-14/?p=34923">MS devblogs article</a>.
     *
     * @param path Target path
     * @param creationTimeAsMS Creation time represented as milliseconds
     * @return true upon success, false otherwise (failure, unsupported attribute, within delta)
     */
    public static boolean updateFileCreationTime(final Path path, final long creationTimeAsMS) throws IOException {
        boolean result = false;
        final BasicFileAttributeView view = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        if (view != null) {
            try {
                final BasicFileAttributes attributes = view.readAttributes();
                final long attributeCreationTimeAsMS = attributes.creationTime().toMillis();  // 0L (usually) means unsupported attribute
                if (attributeCreationTimeAsMS != 0L && 
                        Math.abs(attributeCreationTimeAsMS - creationTimeAsMS) > FILE_CREATION_TIME_MS_MAX_DELTA) {
                    view.setTimes(attributes.lastModifiedTime(), attributes.lastAccessTime(), FileTime.fromMillis(creationTimeAsMS));
                    result = true;  // Successfully changed
                } // Else creationTime unsupported or already within FILE_CREATION_TIME_MS_MAX_DELTA.
            } catch (final UnsupportedOperationException | IOException | SecurityException multiCatchEx) {
                // Possible failures (result false).
            }
        }
        return result;
    }

}
