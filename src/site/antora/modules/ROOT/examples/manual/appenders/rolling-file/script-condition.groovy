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
import java.nio.file.attribute.FileTime
import java.nio.file.*
import java.time.ZonedDateTime
import java.util.stream.Collectors

// tag::script[]
def limit = FileTime.from(ZonedDateTime.now().minusDays(15).toInstant())
def matcher = FileSystems.getDefault().getPathMatcher('glob:app.*.log.gz')
statusLogger.info("Deleting files older than {}.", limit) // <1>
return pathList.stream()
        .filter({
            def relPath = basePath.relativize(it.path) // <2>
            def lastModified = it.attributes.lastModifiedTime()
            Files.isRegularFile(it.path)
                    && lastModified <= limit // <3>
                    && matcher.matches(relPath) // <4>
        })
        .collect(Collectors.toList())
// end::script[]