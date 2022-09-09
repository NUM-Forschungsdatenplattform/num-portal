package de.vitagroup.num.service.policy;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_AQL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.ehrbase.aql.dto.AqlDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import de.vitagroup.num.service.exception.SystemException;

@RunWith(MockitoJUnitRunner.class)
public class TemplatesPolicyTest {

  @Test(expected = SystemException.class)
  public void applyAqlDtoIsNull() {
    Map<String, String> templatesMap = new HashMap<>();
    templatesMap.put("1", "1");
    TemplatesPolicy templatesPolicy = TemplatesPolicy.builder().templatesMap(templatesMap).build();
    when(templatesPolicy.apply(null))
            .thenThrow(new SystemException(TemplatesPolicy.class, INVALID_AQL));
    templatesPolicy.apply(null);
  }

  @Test
  public void applyTemplatesMapIsNull() {
    TemplatesPolicy templatesPolicy = TemplatesPolicy.builder().build();
    assertTrue(templatesPolicy.apply(null));
  }

  @Test
  public void applyAqlDtoIsNotNull() {
    Map<String, String> templatesMap = new HashMap<>();
    templatesMap.put("1", "1");
    TemplatesPolicy templatesPolicy = TemplatesPolicy.builder().templatesMap(templatesMap).build();
    assertTrue(templatesPolicy.apply(new AqlDto()));
  }

}