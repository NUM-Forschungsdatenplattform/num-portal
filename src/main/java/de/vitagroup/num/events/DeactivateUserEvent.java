package de.vitagroup.num.events;

import org.springframework.context.ApplicationEvent;

public class DeactivateUserEvent extends ApplicationEvent {

    private Long organizationId;

    public DeactivateUserEvent(Object source, Long organizationId) {
        super(source);
        this.organizationId = organizationId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

}
