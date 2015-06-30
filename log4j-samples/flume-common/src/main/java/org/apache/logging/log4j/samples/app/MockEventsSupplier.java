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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.samples.dto.AuditEvent;
import org.apache.logging.log4j.samples.events.Alert;
import org.apache.logging.log4j.samples.events.ChangePassword;
import org.apache.logging.log4j.samples.events.Login;
import org.apache.logging.log4j.samples.events.ScheduledTransaction;
import org.apache.logging.log4j.samples.events.Transfer;

public class MockEventsSupplier {

    /* This provides random generation */
    static Random random = new Random();

    public static List<AuditEvent> getAllEvents(final String member) {

        final List<AuditEvent> events = new ArrayList<>();


        final Login login = LogEventFactory.getEvent(Login.class);
        login.setStartPageOption("account summary");
        login.setSource("online");
        login.setMember(member);
        events.add(login);

        final ChangePassword changePassword = LogEventFactory.getEvent(ChangePassword.class);
        changePassword.setMember(member);
        events.add(changePassword);

        final Transfer transfer = LogEventFactory.getEvent(Transfer.class);

        transfer.setAmount("4251");
        transfer.setFromAccount("REPLACE"); // getAccount(mbr, accounts));
        transfer.setToAccount("31142553");
        transfer.setReference("DI-2415220110804");
        transfer.setComment("My Transfer");
        transfer.setMemo("For dinner");
        transfer.setPayment("Use Checking");
        transfer.setTransactionType("1");
        transfer.setSource("IB Transfer page");
        transfer.setCompletionStatus("complete");
        transfer.setMember(member);
        events.add(transfer);

        final Alert alert = LogEventFactory.getEvent(Alert.class);

        alert.setAction("add");
        alert.setType("balance alert");
        alert.setAccountNumber("REPLACE"); // , getAccount(mbr, accounts));
        alert.setTrigger("GT");
        alert.setThreshold("1000");
        alert.setMember(member);
        events.add(alert);

        final ScheduledTransaction scheduledTransaction = LogEventFactory
                .getEvent(ScheduledTransaction.class);

        scheduledTransaction.setAction("add");
        scheduledTransaction.setFromAccount("REPLACE"); // getAccount(mbr,
                                                        // accounts));
        scheduledTransaction.setToAccount("REPLACE"); // "9200000214");
        scheduledTransaction.setAmount("2541");
        scheduledTransaction.setStartDate("20110105");
        scheduledTransaction.setMember("256");
        scheduledTransaction.setFrequency("4");
        scheduledTransaction.setMemo("Scheduled Transfer");
        scheduledTransaction.setPayment("3456");
        scheduledTransaction.setCompletionNotification("Was completed");
        scheduledTransaction.setEndDate("2020-05-30");
        scheduledTransaction.setSrtId("Calabasas2341");
        scheduledTransaction.setSource("Home Page");
        scheduledTransaction.setCompletionStatus("success");
        scheduledTransaction.setMember(member);
        events.add(scheduledTransaction);

        return events;
    }
}
