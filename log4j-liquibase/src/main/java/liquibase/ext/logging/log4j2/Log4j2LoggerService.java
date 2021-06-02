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
package liquibase.ext.logging.log4j2;

import liquibase.logging.Logger;
import liquibase.logging.core.AbstractLogService;

import java.util.HashMap;
import java.util.Map;

/**
 * Logs Liquibase 4.0+ messages to Log4j 2.x.
 */
public class Log4j2LoggerService extends AbstractLogService {

    private static Map<Class, Log4j2Logger> loggers = new HashMap<>();

    @Override
    public int getPriority() {
        return PRIORITY_SPECIALIZED;
    }

    @Override
    public Logger getLog(Class aClass) {
        Log4j2Logger logger = loggers.get(aClass);
        if (logger == null) {
            logger = new Log4j2Logger(aClass, this.filter);
            loggers.put(aClass, logger);
        }

        return logger;
    }
}
