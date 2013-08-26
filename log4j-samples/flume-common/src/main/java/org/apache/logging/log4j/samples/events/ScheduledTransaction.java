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
package org.apache.logging.log4j.samples.events;

/**
 * Member set up scheduled transaction request.
 */

public interface ScheduledTransaction extends org.apache.logging.log4j.samples.dto.AuditEvent {

    /**
     * Action : Indicates the step of the registration process.  Valid actions are: Begin, Submit, Enroll Cancel,
     * Confirm Page, Rt In Process Attempt, Reg submitted, acct del, Account del submit, account auto-Select,
     * Duplicate user.  Alternatively, the action the user has executed in the event
     *
     * @param action Indicates the step of the registration process.  Valid actions are: Begin, Submit, Enroll Cancel,
     *               Confirm Page, Rt In Process Attempt, Reg submitted, acct del, Account del submit,
     *               account auto-Select, Duplicate user.  Alternatively, the action the user has executed in the event
     */
    void setAction(String action);

    /**
     * Amount : Amount of transaction in dollars.
     *
     * @param amount Amount of transaction in dollars.
     */
    void setAmount(String amount);

    /**
     * Completion Notification : Completion notification
     *
     * @param completionNotification Completion notification
     */
    void setCompletionNotification(String completionNotification);

    /**
     * Completion Status : Whether the event succeeded or failed - success/failure and optional reason.
     *
     * @param completionStatus Whether the event succeeded or failed - success/failure and optional reason.
     */
    void setCompletionStatus(String completionStatus);

    /**
     * End Date : Final date for scheduled recurring transfers or PFM history request.
     *
     * @param endDate Final date for scheduled recurring transfers or PFM history request.
     */
    void setEndDate(String endDate);

    /**
     * Frequency : For recurring transactions, payments, the frequency (monthly, weekly) of execution.
     *
     * @param frequency For recurring transactions, payments, the frequency (monthly, weekly) of execution.
     */
    void setFrequency(String frequency);

    /**
     * From Account : For transfer or other transaction, the account funds are taken from.
     *
     * @param fromAccount For transfer or other transaction, the account funds are taken from.
     */
    void setFromAccount(String fromAccount);

    /**
     * Member : Member or End User number at the Host
     *
     * @param member Member or End User number at the Host
     */
    void setMember(String member);

    /**
     * Memo : Descriptive text or memo for transaction
     *
     * @param memo Descriptive text or memo for transaction
     */
    void setMemo(String memo);

    /**
     * Payment : Amount paid or transferred.
     *
     * @param payment Amount paid or transferred.
     */
    void setPayment(String payment);

    /**
     * Source : Source of the End User's request; or method user used to navigate (link, button)
     *
     * @param source Source of the End User's request; or method user used to navigate (link, button)
     */
    void setSource(String source);

    /**
     * SRT Identifier : Scheduled Recurring Transaction Identifier
     *
     * @param srtId Scheduled Recurring Transaction Identifier
     */
    void setSrtId(String srtId);

    /**
     * Start Date : Start date for Scheduled transfers/alerts or PFM history.
     *
     * @param startDate Start date for Scheduled transfers/alerts or PFM history.
     */
    void setStartDate(String startDate);

    /**
     * To Account : Target account or account that will receive funds in a transfer.
     *
     * @param toAccount Target account or account that will receive funds in a transfer.
     */
    void setToAccount(String toAccount);


}