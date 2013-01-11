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
package org.apache.logging.log4j.samples.app;

import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.samples.dto.AuditEvent;
import org.apache.logging.log4j.samples.dto.RequestContext;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


/**
 * The Class LoggingController.
 */
@Controller
public class LoggingController {

    /**
     * The logger.
     */
    private static Logger logger = LogManager.getLogger(LoggingController.class);

    private volatile boolean generateLog = false;
    private final Random ran = new Random();

    private List<AuditEvent> events;

    public LoggingController() {

        ThreadContext.clear();

        RequestContext.setSessionId("session1234");
        RequestContext.setIpAddress("127.0.0.1");
        RequestContext.setClientId("02121");
        RequestContext.setProductName("IB");
        RequestContext.setProductVersion("4.18.1");
        RequestContext.setLocale("en_US");
        RequestContext.setRegion("prod");
    }

    @RequestMapping(value = "/start.do", method = RequestMethod.GET)
    public ModelAndView startLogging(
        @RequestParam(value = "member", required = false, defaultValue = "fakemember") final String member,
                      final HttpServletRequest servletRequest) {

        System.out.println("STARTING..................");

        if (events == null) {
            events = MockEventsSupplier.getAllEvents(member);
        }

        generateLog = true;

        (new Thread() {

            @Override
            public void run() {
                while (generateLog) {
                    // Generate rand number between 1 to 10
                    final int rand = ran.nextInt(9) + 1;

                    // Sleep for rand seconds
                    try {
                        Thread.sleep(rand * 1000);
                    } catch (final InterruptedException e) {
                        logger.warn("WARN", e);
                    }

                    // Write rand number of logs
                    for (int i = 0; i < rand; i++) {
                        final int eventIndex = (Math.abs(ran.nextInt())) % events.size();
                        final AuditEvent event = events.get(eventIndex);
                        RequestContext.setUserId(member);
                        event.logEvent();

                        if ((rand % 4) == 1) {
                            logger.debug("DEBUG level logging.....");
                        } else if ((rand % 4) == 2) {
                            logger.info("INFO level logging.....");
                        } else if ((rand % 4) == 3) {
                            logger.warn("WARN level logging.....");
                        } else {
                            logger.error("ERROR level logging.....");
                        }
                    }

                }
            }
        }).start();

        return new ModelAndView("start.jsp");
    }

    @RequestMapping(value = "/stop.do", method = RequestMethod.GET)
    public ModelAndView stopLogging(final HttpServletRequest servletRequest) {
        generateLog = false;
        return new ModelAndView("stop.jsp");
    }

}
