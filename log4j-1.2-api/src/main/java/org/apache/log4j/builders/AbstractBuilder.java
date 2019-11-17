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
package org.apache.log4j.builders;

import java.util.Properties;

/**
 * Class Description goes here.
 */
public abstract class AbstractBuilder {

    protected static final String FILE_PARAM = "File";
    protected static final String APPEND_PARAM = "Append";
    protected static final String BUFFERED_IO_PARAM = "BufferedIO";
    protected static final String BUFFER_SIZE_PARAM = "BufferSize";
    protected static final String MAX_SIZE_PARAM = "MaxFileSize";
    protected static final String MAX_BACKUP_INDEX = "MaxBackupIndex";
    protected static final String RELATIVE = "RELATIVE";

    private final String prefix;
    private final Properties props;

    public AbstractBuilder() {
        this.prefix = null;
        this.props = new Properties();
    }

    public AbstractBuilder(String prefix, Properties props) {
        this.prefix = prefix + ".";
        this.props = props;
    }

    public String getProperty(String key) {
        return props.getProperty(prefix + key);
    }

    public String getProperty(String key, String defaultValue) {
        return props.getProperty(prefix + key, defaultValue);
    }

    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(props.getProperty(prefix + key, Boolean.FALSE.toString()));
    }
}
