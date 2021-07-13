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

import liquibase.ExtensibleObject;
import liquibase.ObjectMetaData;
import liquibase.logging.LogMessageFilter;
import liquibase.logging.Logger;
import liquibase.logging.core.AbstractLogger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;

import java.util.List;
import java.util.SortedSet;

/**
 * Logs Liquibase messages to Log4j 2.x.
 * <p>
 * This class must be in the {@code liquibase} package in order for the Liquibase plugin discovery mechanism to work.
 * </p>
 *
 * @deprecated This class is used with liquibase versions prior to 4.0 and ignored in 4.0+
 */
public class DeprecatedLog4j2Logger implements Logger {

    private static final String FQCN = DeprecatedLog4j2Logger.class.getName();

    private ExtendedLogger logger;

    public DeprecatedLog4j2Logger() {
    }
    
    @Override
    public void debug(final String message) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, message);
    }

    @Override
    public void debug(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, message, e);
    }

//    @Override //actually overrides a method that existed in 3.x but does not exist in 4.x
    public int getPriority() {
        return 5;
    }

    @Override
    public void info(final String message) {
        logger.logIfEnabled(FQCN, Level.INFO, null, message);
    }

    @Override
    public void info(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.INFO, null, message, e);
    }

    //    @Override //actually overrides a method that existed in 3.x but does not exist in 4.x
    public void setLogLevel(final String logLevel, final String logFile) {
        // cannot set logLevel programmatically
        // ignore logFile
    }

    //    @Override //actually overrides a method that existed in 3.x but does not exist in 4.x
    public void setName(final String name) {
        logger = LogManager.getContext(false).getLogger(name);
    }

    @Override
    public void severe(final String message) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, message);
    }

    @Override
    public void severe(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, message, e);
    }

    @Override
    public void warning(final String message) {
        logger.logIfEnabled(FQCN, Level.WARN, null, message);
    }

    @Override
    public void warning(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.WARN, null, message, e);
    }

    @Override
    public void log(java.util.logging.Level level, String s, Throwable throwable) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
    }

    @Override
    public void close() throws Exception {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
    }

    @Override
    public void config(String s) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
    }

    @Override
    public void config(String s, Throwable throwable) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
    }

    @Override
    public void fine(String s) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
    }

    @Override
    public void fine(String s, Throwable throwable) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
    }

    @Override
    public SortedSet<String> getAttributes() {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.

        return null;
    }

    @Override
    public ObjectMetaData getObjectMetaData() {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return null;
    }

    @Override
    public boolean has(String s) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return false;
    }

    @Override
    public List getValuePath(String s, Class aClass) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return null;
    }

    @Override
    public <T> T get(String s, Class<T> aClass) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return null;
    }

    @Override
    public <T> T get(String s, T t) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return null;
    }

    @Override
    public ExtensibleObject set(String s, Object o) {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return null;
    }

    @Override
    public String describe() {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return null;
    }

    @Override
    public Object clone() {
        //method exists to conform to the 4.x parent class, but is not used in the 3.x range where this class will be loaded.
        return null;
    }
}
