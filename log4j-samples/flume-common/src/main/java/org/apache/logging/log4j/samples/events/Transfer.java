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

import org.apache.logging.log4j.samples.dto.Constraint;

/**
 * Member requested transfer.
 */

public interface Transfer extends org.apache.logging.log4j.samples.dto.AuditEvent {

    /**
     * Amount : Amount of transaction in dollars.
     *
     * @param amount Amount of transaction in dollars.
     */
    @Constraint(required = true)
    void setAmount(String amount);

    /**
     * Comment : Comment
     *
     * @param comment Comment
     */
    void setComment(String comment);

    /**
     * Completion Status : Whether the event succeeded or failed - success/failure and optional reason.
     *
     * @param completionStatus Whether the event succeeded or failed - success/failure and optional reason.
     */
    void setCompletionStatus(String completionStatus);

    /**
     * Confirmation Number : A date, time, and reference number.
     *
     * @param confNo A date, time, and reference number.
     */
    void setConfNo(String confNo);

    /**
     * From Account : For transfer or other transaction, the account funds are taken from.
     *
     * @param fromAccount For transfer or other transaction, the account funds are taken from.
     */
    @Constraint(required = true)
    void setFromAccount(String fromAccount);

    /**
     * From Account Type : For transfer or other transaction, the type of the account funds are taken from.
     *
     * @param fromAccountType For transfer or other transaction, the type of the account funds are taken from.
     */
    void setFromAccountType(String fromAccountType);

    /**
     * From Routing Number : nine digit bank code to ID the FI from which funds are taken
     *
     * @param fromRoutingNumber nine digit bank code to ID the FI from which funds are taken
     */
    void setFromRoutingNumber(String fromRoutingNumber);

    /**
     * Member : Member or End User number at the Host
     *
     * @param member Member or End User number at the Host
     */
    @Constraint(required = true)
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
     * Reference : Unique reference number or identifier for transfers.
     *
     * @param reference Unique reference number or identifier for transfers.
     */
    void setReference(String reference);

    /**
     * Result : Status of request event.
     *
     * @param result Status of request event.
     */
    void setResult(String result);

    /**
     * Source : Source of the End User's request; or method user used to navigate (link, button)
     *
     * @param source Source of the End User's request; or method user used to navigate (link, button)
     */
    void setSource(String source);

    /**
     * Status : Status of promotional email preference or online statement.
     *
     * @param status Status of promotional email preference or online statement.
     */
    void setStatus(String status);

    /**
     * Time Stamp : Time Stamp of event.
     *
     * @param timeStamp Time Stamp of event.
     */
    void setTimeStamp(long timeStamp);

    /**
     * To Account : Target account or account that will receive funds in a transfer.
     *
     * @param toAccount Target account or account that will receive funds in a transfer.
     */
    @Constraint(required = true)
    void setToAccount(String toAccount);

    /**
     * To Account Type : Target account type such as savings or checking.
     *
     * @param toAccountType Target account type such as savings or checking.
     */
    void setToAccountType(String toAccountType);

    /**
     * To Routing Number : nine digit bank code to ID the FI to receive funds
     *
     * @param toRoutingNumber nine digit bank code to ID the FI to receive funds
     */
    void setToRoutingNumber(String toRoutingNumber);

    /**
     * Transaction Type : Type of transfer, i.e., withdrawal, LOC Paydown, CD redemption, lucky transfer.
     *
     * @param transactionType Type of transfer, i.e., withdrawal, LOC Paydown, CD redemption, lucky transfer.
     */
    void setTransactionType(String transactionType);

    /**
     * Type : Type of event, bill payment, balance, application, or attribute. For bill pay, type of payment (check, electronic)
     *
     * @param type Type of event, bill payment, balance, application, or attribute. For bill pay, type of payment (check, electronic)
     */
    void setType(String type);


}