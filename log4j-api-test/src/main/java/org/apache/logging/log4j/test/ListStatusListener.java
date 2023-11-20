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
package org.apache.logging.log4j.test;

import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@link StatusListener} that collects messages for further inspection.
 */
@ProviderType
public interface ListStatusListener extends StatusListener {

    void clear();

    Stream<StatusData> getStatusData();

    default Stream<StatusData> findStatusData(Level level) {
        return getStatusData().filter(data -> level.isLessSpecificThan(data.getLevel()));
    }

    default Stream<StatusData> findStatusData(Level level, String regex) {
        return findStatusData(level)
                .filter(data -> data.getMessage().getFormattedMessage().matches(regex));
    }
}
