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
package org.apache.logging.log4j.core.layout.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 *
 */
@Plugin(name="IETFSyslogStartPatternConverter", type="Converter")
@ConverterKeys({"n","syslogStart"})
public class IETFSyslogStartConverter extends LogEventPatternConverter {

  private static final DecimalFormat TWO_DIGIT = new DecimalFormat("00");
  private static final DecimalFormat FOUR_DIGIT = new DecimalFormat("0000");

  long lastTimestamp = -1;
  String timesmapStr = null;
  SimpleDateFormat simpleFormat;
  String localHostName;
  int facility;

  String appName;
  String messageId;

  public void start() {
    int errorCount = 0;

    String facilityStr = getFirstOption();
    if (facilityStr == null) {
      addError("was expecting a facility string as an option");
      return;
    }
    facility = SyslogAppenderBase.facilityStringToint(facilityStr);

    Map<SyslogOption, String> options = ConverterOptions.getOptions(SyslogOption.class, getOptionList());

    for (Map.Entry<SyslogOption, String> entry : options.entrySet()) {
      switch (entry.getKey()) {
        case APPNAME:
          appName = entry.getValue();
          break;
        case MESSAGEID:
          messageId = entry.getValue();
          break;
      }
    }

    localHostName = getLocalHostname();
    try {
      simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    } catch (IllegalArgumentException e) {
      addError("Could not instantiate SimpleDateFormat", e);
      errorCount++;
    }

    if(errorCount == 0) {
      super.start();
    }
  }

  public void format(LogEvent event, final StringBuilder toAppendTo) {

    int pri = facility + LevelToSyslogSeverity.convert(event);

    toAppendTo.append("<");
    toAppendTo.append(pri);
    toAppendTo.append(">1 ");
    toAppendTo.append(computeTimeStampString(event.getMillis()));
    toAppendTo.append(' ');
    toAppendTo.append(localHostName);
    toAppendTo.append(' ');
    if (appName != null) {
      toAppendTo.append(appName);
    /* } else if (event.getLoggerContextVO().getName() != null) {
      toAppendTo.append(event.getLoggerContextVO().getName()); */
    } else {
      toAppendTo.append("-");
    }
    toAppendTo.append(" ");
    toAppendTo.append(getProcId());
    toAppendTo.append(" ");
    String type = getStructuredType(event.getMessage());
    if (type != null) {
      toAppendTo.append(type);
    } else if (messageId != null) {
      toAppendTo.append(messageId);
    } else {
      toAppendTo.append("-");
    }
    toAppendTo.append(" ");
    return toAppendTo.toString();
  }

  private String getStructuredType(Message msg) {
    if (msg == null || !(msg instanceof StructuredDataMessage)) {
      return null;
    }
    return ((StructuredDataMessage) msg).getType();
  }

  String getProcId() {
    return "-";
  }

  /**
   * This method gets the network name of the machine we are running on.
   * Returns "UNKNOWN_LOCALHOST" in the unlikely case where the host name
   * cannot be found.
   * @return String the name of the local host
   */
  public String getLocalHostname() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      return addr.getHostName();
    } catch (UnknownHostException uhe) {
      addError("Could not determine local host name", uhe);
      return "UNKNOWN_LOCALHOST";
    }
  }

  String computeTimeStampString(long now) {
    synchronized (this) {
      if (now != lastTimestamp) {
        lastTimestamp = now;
        StringBuilder buf = new StringBuilder();
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(now);
        buf.append(FOUR_DIGIT.format(cal.get(Calendar.YEAR)));
        buf.append("-");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.MONTH) + 1));
        buf.append("-");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.DAY_OF_MONTH)));
        buf.append("T");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.HOUR_OF_DAY)));
        buf.append(":");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.MINUTE)));
        buf.append(":");
        buf.append(TWO_DIGIT.format(cal.get(Calendar.SECOND)));

        int millis = cal.get(Calendar.MILLISECOND);
        if (millis != 0) {
          buf.append(".").append((int) ((float) millis / 10F));
        }

        int tzmin = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
        if (tzmin == 0) {
          buf.append("Z");
        } else {
          if (tzmin < 0) {
            tzmin = -tzmin;
            buf.append("-");
          } else {
            buf.append("+");
          }
          int tzhour = tzmin / 60;
          tzmin -= tzhour * 60;
          buf.append(TWO_DIGIT.format(tzhour));
          buf.append(":");
          buf.append(TWO_DIGIT.format(tzmin));
        }
        timesmapStr = buf.toString();
      }
      return timesmapStr;
    }
  }
}