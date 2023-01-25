package de.vitagroup.num.listeners;

import de.vitagroup.num.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserCacheInit implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    UserService userService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("---- start load existing users into cache ----- ");
        userService.initializeUsersCache();
        log.info("---- end load existing users into cache ----- ");
    }
}
