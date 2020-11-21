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
package org.apache.logging.log4j.spring.cloud.config.sample.controller;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.apache.logging.log4j.util.Timer;



@RestController
public class SampleController {

    private static final Logger LOGGER = LogManager.getLogger(SampleController.class);
    private static int MAX_MESSAGES = 100000;

    @GetMapping("/log")
    public ResponseEntity<String> get(@RequestParam(name="threads", defaultValue="1") int threads,
        @RequestParam(name="messages", defaultValue="100000") int count) {
        if (threads < 1) {
            threads = 1;
        }
        if (count < threads) {
            count = threads * 10000;
        }
        if ((count * threads) > MAX_MESSAGES) {
            count = MAX_MESSAGES / threads;
        }
        String msg = "";
        if (threads == 1) {
            Timer timer = new Timer("sample");
            timer.start();
            for (int n = 0; n < count; ++n) {
                LOGGER.info("Log record " + n);
            }
            timer.stop();
            StringBuilder sb = new StringBuilder("Elapsed time = ");
            timer.formatTo(sb);
            msg = sb.toString();
        } else {
            ExecutorService service = Executors.newFixedThreadPool(threads);
            Timer timer = new Timer("sample");
            timer.start();
            for (int i = 0; i < threads; ++i) {
                service.submit(new Worker(i, count));
            }
            service.shutdown();
            try {
                service.awaitTermination(2, TimeUnit.MINUTES);
                timer.stop();
                StringBuilder sb = new StringBuilder("Elapsed time = ");
                timer.formatTo(sb);
                msg = sb.toString();
            } catch (InterruptedException ex) {
                msg = "Max time exceeded";
            }
        }

        return ResponseEntity.ok(msg);
    }

    @GetMapping("/exception")
    public ResponseEntity<String> exception() {
        Throwable t = new Throwable("This is a test");
        LOGGER.info("This is a test", t);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        t.printStackTrace(ps);
        String stackTrace = os.toString();
        stackTrace = stackTrace.replaceAll("\n", "<br>");

        //LOGGER.info("Hello, World");
        return ResponseEntity.ok(stackTrace);
    }

    private static class Worker implements Runnable {

        private final int id;
        private final int count;

        public Worker(int id, int count) {
            this.id = id;
            this.count = count;
        }

        @Override
        public void run() {
            String prefix = "Thread " + id + " record ";
            for (int i = 0; i < count; ++i) {
                LOGGER.info(prefix + i);
            }
        }
    }
}
