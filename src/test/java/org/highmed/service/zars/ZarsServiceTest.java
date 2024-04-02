package org.highmed.service.zars;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.highmed.domain.dto.ZarsInfoDto;
import org.highmed.domain.model.ProjectCategories;
import org.highmed.domain.model.ProjectStatus;
import org.highmed.service.email.EmailService;
import org.highmed.service.email.ZarsProperties;
import org.highmed.service.email.ZarsService;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ZarsServiceTest {

  MemoryAppender memoryAppender;
  @Spy private ObjectMapper objectMapper;
  @Mock private EmailService emailService;
  @Mock private ZarsProperties zarsProperties;
  @InjectMocks private ZarsService zarsService;
  @Spy private final MessageSource messageSource = new TestMessageSource();

  @Before
  public void setUp() {
    zarsService.initialize();

    Logger logger = (Logger) LoggerFactory.getLogger(ZarsService.class);
    memoryAppender = new MemoryAppender();
    memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    logger.setLevel(Level.DEBUG);
    logger.addAppender(memoryAppender);
    memoryAppender.start();
  }

  @Test
  public void shouldRegisterToZars() {
    ZarsInfoDto zarsInfoDto = new ZarsInfoDto();
    zarsInfoDto.setCategories(
        Set.of(ProjectCategories.DECISION_SUPPORT, ProjectCategories.MICROBIOLOGY));
    zarsInfoDto.setKeywords(Set.of("keyword1", "keyword2"));
    zarsInfoDto.setApprovalDate("24.12.2022");
    zarsInfoDto.setStartDate(LocalDate.now());
    zarsInfoDto.setEndDate(LocalDate.now());
    zarsInfoDto.setCoordinator("Coordinator");
    zarsInfoDto.setId(1);
    zarsInfoDto.setName("Project1");
    zarsInfoDto.setStatus(ProjectStatus.APPROVED);
    zarsService.registerToZars(zarsInfoDto);

    assertTrue(
        memoryAppender.contains("Registration email successfully sent to null", Level.DEBUG));
  }
}
