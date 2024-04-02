package org.highmed.numportal.listeners;

import org.highmed.numportal.events.DeactivateUserEvent;
import org.highmed.numportal.service.UserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeactivateUserListener implements ApplicationListener<DeactivateUserEvent> {

    private final UserDetailsService userDetailsService;

    @Async
    @Override
    public void onApplicationEvent(DeactivateUserEvent event) {
        log.info("Deactivate users for organization {} was triggered by {} ", event.getOrganizationId(), event.getLoggedInUserId());
        userDetailsService.deactivateUsers(event.getLoggedInUserId(), event.getOrganizationId());
    }
}
