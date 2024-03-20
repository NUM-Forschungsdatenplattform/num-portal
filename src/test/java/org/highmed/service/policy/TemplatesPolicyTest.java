package org.highmed.service.policy;

import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.highmed.service.exception.SystemException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class TemplatesPolicyTest {

  @Test(expected = SystemException.class)
  public void applyAqlDtoIsNull() {
    Map<String, String> templatesMap = new HashMap<>();
    templatesMap.put("1", "1");
    TemplatesPolicy templatesPolicy = TemplatesPolicy.builder().templatesMap(templatesMap).build();
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
    assertTrue(templatesPolicy.apply(AqlQueryParser.parse("select e from Ehr e")));
  }

}
