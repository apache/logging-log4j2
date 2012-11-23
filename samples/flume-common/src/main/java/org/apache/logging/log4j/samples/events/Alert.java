package org.apache.logging.log4j.samples.events;

import org.apache.logging.log4j.samples.dto.Constraint;

/**
 * The user sets up account balance alerts.
 */

public interface Alert extends org.apache.logging.log4j.samples.dto.AuditEvent {

    /**
     * Account Number : Account number
     *
     * @param accountNumber Account number
     */
    @Constraint(required = true)
    void setAccountNumber(String accountNumber);

    /**
     * Action : Indicates the step of the registration process.  Valid actions are: Begin, Submit, Enroll Cancel, Confirm Page, Rt In Process Attempt, Reg submitted, acct del, Account del submit, account auto-Select, Duplicate user.  Alternatively, the action the user has executed in the event
     *
     * @param action Indicates the step of the registration process.  Valid actions are: Begin, Submit, Enroll Cancel, Confirm Page, Rt In Process Attempt, Reg submitted, acct del, Account del submit, account auto-Select, Duplicate user.  Alternatively, the action the user has executed in the event
     */
    void setAction(String action);

    /**
     * Completion Status : Whether the event succeeded or failed - success/failure and optional reason.
     *
     * @param completionStatus Whether the event succeeded or failed - success/failure and optional reason.
     */
    void setCompletionStatus(String completionStatus);

    /**
     * Member : Member or End User number at the Host
     *
     * @param member Member or End User number at the Host
     */
    @Constraint(required = true)
    void setMember(String member);

    /**
     * Threshold : Balance alert, the amount to compare against the balance in the case of a balance alert.  Check cleared alert, there is no value.  for maturity date alert, there is no value.  Loan payment due alert, there is no value.  Loan payment past due alert, there is no value.  Personal reminder and periodic balance alerts, this date is the Start Date that the alert will begin to be sent on.
     *
     * @param threshold Balance alert, the amount to compare against the balance in the case of a balance alert.  Check cleared alert, there is no value.  for maturity date alert, there is no value.  Loan payment due alert, there is no value.  Loan payment past due alert, there is no value.  Personal reminder and periodic balance alerts, this date is the Start Date that the alert will begin to be sent on.
     */
    void setThreshold(String threshold);

    /**
     * Trigger : Balance alert, the value is an operator ('GT' for Greater Than or 'LT' for Less Than).  Check alert, this value is the check number.  Maturity date alert, the value is the number of days prior to maturity.  Loan payment due alert, the value is the number of days prior to payment due date.  Loan payment past due alert, there is no value.  Personal reminder and periodic balance alerts, this text is the frequency that the alert will be sent.
     *
     * @param trigger Balance alert, the value is an operator ('GT' for Greater Than or 'LT' for Less Than).  Check alert, this value is the check number.  Maturity date alert, the value is the number of days prior to maturity.  Loan payment due alert, the value is the number of days prior to payment due date.  Loan payment past due alert, there is no value.  Personal reminder and periodic balance alerts, this text is the frequency that the alert will be sent.
     */
    void setTrigger(String trigger);

    /**
     * Type : Type of event, bill payment, balance, application, or attribute. For bill pay, type of payment (check, electronic)
     *
     * @param type Type of event, bill payment, balance, application, or attribute. For bill pay, type of payment (check, electronic)
     */
    @Constraint(required = true)
    void setType(String type);
}