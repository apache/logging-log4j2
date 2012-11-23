package org.apache.logging.log4j.samples.events;

/**
 * Member logged in successfully.
 *
 * @author generated
 */

public interface Login extends org.apache.logging.log4j.samples.dto.AuditEvent {

    /**
     * Member : Member or End User number at the Host
     *
     * @param member Member or End User number at the Host
     */
    void setMember(String member);

    /**
     * Source : Source of the End User's request; or method user used to navigate (link, button)
     *
     * @param source Source of the End User's request; or method user used to navigate (link, button)
     */
    void setSource(String source);

    /**
     * Start Page Option : Chosen start page destination for IB login.
     *
     * @param startPageOption Chosen start page destination for IB login.
     */
    void setStartPageOption(String startPageOption);

}