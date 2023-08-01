package de.vitagroup.num.listeners;

import de.vitagroup.num.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserCacheInit implements ApplicationListener<ApplicationReadyEvent> {

    private final UserService userService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("---- start load existing users into cache ----- ");
        userService.initializeUsersCache();
        userService.initializeTranslationCache();
        log.info("---- end load existing users into cache ----- ");
    }
}
