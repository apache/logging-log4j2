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
package org.apache.logging.log4j.hamcrest;

import java.io.File;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

/**
 * Hamcrest Matchers that operate on File objects.
 *
 * @since 2.1
 */
public final class FileMatchers {

    /**
     * Matches if the File exists.
     *
     * @return the Matcher.
     */
    public static Matcher<File> exists() {
        return new FeatureMatcher<File, Boolean>(is(true), "file exists", "file exists") {
            @Override
            protected Boolean featureValueOf(final File actual) {
                return actual.exists();
            }
        };
    }

    /**
     * Matches a Long Matcher against the file length.
     *
     * @param matcher the Matcher to use on the File length.
     * @return the Matcher.
     */
    public static Matcher<File> hasLength(final Matcher<Long> matcher) {
        return new FeatureMatcher<File, Long>(matcher, "file with size", "file with size") {
            @Override
            protected Long featureValueOf(final File actual) {
                return actual.length();
            }
        };
    }

    /**
     * Matches a specific file length.
     *
     * @param length the file length to match
     * @param <T>    the type of the length.
     * @return the Matcher.
     */
    public static <T extends Number> Matcher<File> hasLength(final T length) {
        return hasLength(equalTo(length.longValue()));
    }

    /**
     * Matches a specific file length.
     *
     * @param length the file length to match
     * @return the Matcher.
     */
    public static Matcher<File> hasLength(final long length) {
        return hasLength(equalTo(length));
    }

    /**
     * Matches a file with length equal to zero.
     *
     * @return the Matcher.
     */
    public static Matcher<File> isEmpty() {
        return hasLength(0L);
    }

    /**
     * Matches against a file's last modification time in milliseconds.
     *
     * @param matcher the Matcher to use on the File modification time.
     * @return the Matcher.
     */
    public static Matcher<File> lastModified(final Matcher<Long> matcher) {
        return new FeatureMatcher<File, Long>(matcher, "was last modified", "was last modified") {
            @Override
            protected Long featureValueOf(final File actual) {
                return actual.lastModified();
            }
        };
    }

    /**
     * Matches a time in millis before the current time.
     *
     * @return the Matcher.
     */
    public static Matcher<Long> beforeNow() {
        return lessThanOrEqualTo(System.currentTimeMillis());
    }

    /**
     * Matches the number of files in a directory.
     *
     * @param matcher the Matcher to use on the number of files.
     * @return the Matcher.
     */
    public static Matcher<File> hasNumberOfFiles(final Matcher<Integer> matcher) {
        return new FeatureMatcher<File, Integer>(matcher, "directory with number of files",
            "directory with number of files") {
            @Override
            protected Integer featureValueOf(final File actual) {
                final File[] files = actual.listFiles();
                return files == null ? 0 : files.length;
            }
        };
    }

    /**
     * Matches a directory with at least one file.
     *
     * @return the Matcher.
     */
    public static Matcher<File> hasFiles() {
        return hasNumberOfFiles(greaterThan(0));
    }

    /**
     * Matches a file name.
     *
     * @param matcher the Matcher to use on the file name.
     * @return the Matcher.
     */
    public static Matcher<File> hasName(final Matcher<String> matcher) {
        return new FeatureMatcher<File, String>(matcher, "file named", "file named") {
            @Override
            protected String featureValueOf(final File actual) {
                return actual.getName();
            }
        };
    }

    private FileMatchers() {
    }

}
