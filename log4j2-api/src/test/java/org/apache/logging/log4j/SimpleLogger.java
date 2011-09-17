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
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SimpleLogger extends AbstractLogger {
    private List<String> array = new ArrayList<String>();

    @Override
    protected String getFQCN() {
        return SimpleLogger.class.getName();
    }

    public List<String> getEntries() {
        return array;
    }

    @Override
    public void log(Marker marker, String fqcn, Level level, Message msg, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ");
        sb.append(level.toString());
        sb.append(" ");
        sb.append(msg.getFormattedMessage());
        Map<String, Object> mdc = ThreadContext.getContext();
        if (mdc.size() > 0) {
            sb.append(" ");
            sb.append(mdc.toString());
            sb.append(" ");
        }
        Object[] params = msg.getParameters();
        Throwable t;
        if (throwable == null && params != null && params[params.length -1] instanceof Throwable ) {
            t = (Throwable) params[params.length - 1];
        } else {
            t = throwable;
        }
        if (t != null) {
            sb.append(" ");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            sb.append(baos.toString());
        }
        array.add(sb.toString());
        //System.out.println(sb.toString());
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2, Object p3,
                                Object...params) {
        return true;
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg) {
        return true;
    }


    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Throwable t) {
        return true;
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1) {
        return true;
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2) {
        return true;
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2, Object p3) {
        return true;
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Object msg, Throwable t) {
        return true;
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Message msg, Throwable t) {
        return true;
    }
}
