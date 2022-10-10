package de.vitagroup.num.listeners;

import de.vitagroup.num.domain.templates.ExceptionsTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExceptionMapInit implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        new ExceptionsTemplate().populateExceptionMap();
    }
}
