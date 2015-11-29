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

package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/**
 * Tuple of a {@code Path} and {@code BasicFileAttributes}, used for sorting.
 */
public class PathWithAttributes implements BasicFileAttributes {

    private final Path path;
    private final BasicFileAttributes attributes;
    private final FileTime lastModifiedTime;
    private final FileTime lastAccessTime;
    private final FileTime creationTime;
    private final boolean regularFile;
    private final boolean directory;
    private final boolean symbolicLink;
    private final boolean other;
    private final long size;
    private final Object fileKey;

    public PathWithAttributes(final Path path, final BasicFileAttributes attributes) {
        this.path = Objects.requireNonNull(path, "path");
        this.attributes = Objects.requireNonNull(attributes, "attributes");

        // take snapshot of attributes, it may be just a view whose values change
        this.lastModifiedTime = attributes.lastModifiedTime();
        this.lastAccessTime = attributes.lastAccessTime();
        this.creationTime = attributes.creationTime();
        this.regularFile = attributes.isRegularFile();
        this.directory = attributes.isDirectory();
        this.symbolicLink = attributes.isSymbolicLink();
        this.other = attributes.isOther();
        this.size = attributes.size();
        this.fileKey = attributes.fileKey();
    }

    @Override
    public String toString() {
        return path + " (created: " + creationTime + ", modified: " + lastModifiedTime + ", attrMod="
                + attributes.lastModifiedTime() + ")";
    }

    /**
     * Returns the path.
     * 
     * @return the path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the attributes.
     * 
     * @return the attributes
     */
    public BasicFileAttributes getAttributes() {
        return this;
    }

    /**
     * Returns the original attributes object.
     * 
     * @return the original attributes object
     */
    public BasicFileAttributes getOriginalAttributes() {
        return attributes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#lastModifiedTime()
     */
    @Override
    public FileTime lastModifiedTime() {
        return lastModifiedTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#lastAccessTime()
     */
    @Override
    public FileTime lastAccessTime() {
        return lastAccessTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#creationTime()
     */
    @Override
    public FileTime creationTime() {
        return creationTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#isRegularFile()
     */
    @Override
    public boolean isRegularFile() {
        return regularFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        return directory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#isSymbolicLink()
     */
    @Override
    public boolean isSymbolicLink() {
        return symbolicLink;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#isOther()
     */
    @Override
    public boolean isOther() {
        return other;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#size()
     */
    @Override
    public long size() {
        return size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.nio.file.attribute.BasicFileAttributes#fileKey()
     */
    @Override
    public Object fileKey() {
        return fileKey;
    }
}
