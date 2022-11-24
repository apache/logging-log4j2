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
package org.apache.logging.log4j.internal.util.changelog.releaser;

import java.nio.file.Path;
import java.nio.file.Paths;

final class ChangelogReleaserArgs {

    final Path projectRootDirectory;

    private ChangelogReleaserArgs(final Path projectRootDirectory) {
        this.projectRootDirectory = projectRootDirectory;
    }

    static ChangelogReleaserArgs fromMainArgs(final String[] args) {
        if (args.length != 1) {
            final String s = String.format("invalid number of arguments: %d, was expecting: <projectRootPath>", args.length);
            throw new IllegalArgumentException(s);
        }
        final Path projectRootPath = Paths.get(args[0]);
        return new ChangelogReleaserArgs(projectRootPath);
    }

}
