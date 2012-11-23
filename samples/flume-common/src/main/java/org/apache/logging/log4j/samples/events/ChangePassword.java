package org.apache.logging.log4j.samples.events;

import org.apache.logging.log4j.samples.dto.Constraint;

/**
 * Member change their password.
 *
 * @author generated
 */

public interface ChangePassword extends org.apache.logging.log4j.samples.dto.AuditEvent {

    /**
     * Member : Member or End User number at the Host
     *
     * @param member Member or End User number at the Host
     */
    @Constraint(required = true)
    void setMember(String member);
}