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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Test helper class: file attributes.
 */
public class DummyFileAttributes implements BasicFileAttributes {

    public FileTime lastModified;
    public FileTime lastAccessTime;
    public FileTime creationTime;
    public boolean isRegularFile;
    public boolean isDirectory;
    public boolean isSymbolicLink;
    public boolean isOther;
    public long size;
    public Object fileKey;
    
    public DummyFileAttributes() {
    }

    @Override
    public FileTime lastModifiedTime() {
        return lastModified;
    }

    @Override
    public FileTime lastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public FileTime creationTime() {
        return creationTime;
    }

    @Override
    public boolean isRegularFile() {
        return isRegularFile;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isSymbolicLink() {
        return isSymbolicLink;
    }

    @Override
    public boolean isOther() {
        return isOther;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Object fileKey() {
        return fileKey;
    }

}
