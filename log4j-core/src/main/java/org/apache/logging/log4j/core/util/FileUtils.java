/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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
package org.apache.logging.log4j.core.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final Logger LOGGER = StatusLogger.getLogger();

    private FileUtils() {}

    /**
     * Tries to convert the specified URI to a file object. If this fails, <b>null</b> is returned.
     *
     * @param uri the URI
     * @return the resulting file object
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Currently `uri` comes from a configuration file.")
    public static File fileFromUri(URI uri) {
        if (uri == null) {
            return null;
        }
        if (uri.isAbsolute()) {
            if (JBOSS_FILE.equals(uri.getScheme())) {
                try {
                    // patch the scheme
                    uri = new URI(PROTOCOL_FILE, uri.getSchemeSpecificPart(), uri.getFragment());
                } catch (URISyntaxException use) {
                    // should not happen, ignore
                }
            }
            try {
                if (PROTOCOL_FILE.equals(uri.getScheme())) {
                    return new File(uri);
                }
            } catch (final Exception ex) {
                LOGGER.warn("Invalid URI {}", uri);
            }
        } else {
            final File file = new File(uri.toString());
            try {
                if (file.exists()) {
                    return file;
                }
                final String path = uri.getPath();
                return new File(path);
            } catch (final Exception ex) {
                LOGGER.warn("Invalid URI {}", uri);
            }
        }
        return null;
    }

    public static boolean isFile(final URL url) {
        return url != null
                && (url.getProtocol().equals(PROTOCOL_FILE) || url.getProtocol().equals(JBOSS_FILE));
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

        if (!dir.exists() && !createDirectoryIfNotExisting) {
            throw new IOException("The directory " + dir.getAbsolutePath() + " does not exist.");
        }

        try {
            Files.createDirectories(dir.toPath());
        } catch (FileAlreadyExistsException e) {
            if (!dir.isDirectory()) {
                throw new IOException("File " + dir + " exists and is not a directory. Unable to create directory.");
            }
        } catch (Exception e) {
            throw new IOException("Could not create directory " + dir.getAbsolutePath());
        }
    }

    /**
     * Creates the parent directories for the given File.
     *
     * @param file For which parent directory is to be created.
     * @throws IOException Thrown if the directory could not be created.
     */
    public static void makeParentDirs(final File file) throws IOException {
        final File parent =
                Objects.requireNonNull(file, "file").getCanonicalFile().getParentFile();
        if (parent != null) {
            mkdir(parent, true);
        }
    }

    /**
     * Define file POSIX attribute view on a path/file.
     *
     * @param path Target path
     * @param filePermissions Permissions to apply
     * @param fileOwner File owner
     * @param fileGroup File group
     * @throws IOException If IO error during definition of file attribute view
     */
    public static void defineFilePosixAttributeView(
            final Path path,
            final Set<PosixFilePermission> filePermissions,
            final String fileOwner,
            final String fileGroup)
            throws IOException {
        final PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (view != null) {
            final UserPrincipalLookupService lookupService =
                    FileSystems.getDefault().getUserPrincipalLookupService();
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
     * Check if POSIX file attribute view is supported on the default FileSystem.
     *
     * @return true if POSIX file attribute view supported, false otherwise
     */
    public static boolean isFilePosixAttributeViewSupported() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }
}
