package de.vitagroup.num.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.vitagroup.num.NumPortalApplication;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NumPortalApplication.class)
@RequiredArgsConstructor
class EhrBaseServiceTest {

  @Autowired
  private EhrBaseService ehrBaseService;

  @Test
  void getPatientIds() {
    String aql = "SELECT e/ehr_id/value FROM EHR e";
    List<String> patients = ehrBaseService.getPatientIds(aql);
    assertThat(patients).isNotNull().isNotEmpty();
  }
}
