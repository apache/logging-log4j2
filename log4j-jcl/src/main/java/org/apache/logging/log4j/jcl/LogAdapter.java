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
package org.apache.logging.log4j.jcl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.ReflectionUtil;

/**
 * Commons Logging adapter registry.
 *
 * @since 2.1
 */
public class LogAdapter extends AbstractLoggerAdapter<Log> {

    @Override
    protected Log newLogger(final String name, final LoggerContext context) {
        return new Log4jLog(context.getLogger(name));
    }

    @Override
    protected LoggerContext getContext() {
        return getContext(ReflectionUtil.getCallerClass(LogFactory.class));
    }

}
