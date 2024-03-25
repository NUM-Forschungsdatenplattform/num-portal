package org.highmed.events;

import org.springframework.context.ApplicationEvent;

public class DeactivateUserEvent extends ApplicationEvent {

    private Long organizationId;

    private String loggedInUserId;

    public DeactivateUserEvent(Object source, Long organizationId, String loggedInUserId) {
        super(source);
        this.organizationId = organizationId;
        this.loggedInUserId = loggedInUserId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getLoggedInUserId() {
        return loggedInUserId;
    }
}
