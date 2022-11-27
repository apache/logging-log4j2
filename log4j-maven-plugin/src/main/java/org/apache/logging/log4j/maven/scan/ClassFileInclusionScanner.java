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
package org.apache.logging.log4j.maven.scan;

import java.nio.file.Path;
import java.util.Set;

public interface ClassFileInclusionScanner {

    /**
     * Finds class files matching a specific condition.
     *
     * @param sourceDir path to the folder where to search files
     * @param targetDir an auxiliary folder
     * @return a set of relative paths to file in {@code sourceDir}
     */
    Set<Path> getIncludedClassFiles(Path sourceDir, Path targetDir);
}
