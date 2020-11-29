package de.vitagroup.num.domain;

public enum StudyStatus {
    /**
     * Study creation in progress
     */
    DRAFT,

    /**
     * Study is finalized and send for approval but still editable
     */
    PENDING,

    /**
     * Study is being review and cannot be edited
     */
    REVIEWING,

    /**
     * Pending requests from the reviewer
     */
    CHANGE_REQUEST,

    /**
     * Study is denied
     */
    DENIED,

    /**
     * Study is approved
     */
    APPROVED,

    /**
     * Study is published and cannot be edited anymore
     */
    PUBLISHED,

    /**
     * Study is finished
     */
    CLOSED
}
