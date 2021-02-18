package de.vitagroup.num.service.ehrbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import de.vitagroup.num.web.exception.SystemException;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.ehrbase.aqleditor.service.TestDataTemplateProvider;
import org.ehrbase.serialisation.flatencoding.FlatFormat;
import org.ehrbase.serialisation.flatencoding.FlatJasonProvider;
import org.ehrbase.serialisation.flatencoding.FlatJson;
import org.ehrbase.util.exception.SdkException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CompositionFlattener {

  private final ObjectMapper mapper;

  public Map<String, String> flatten(Composition composition) {

    validateComposition(composition);

    try {
      String templateId = composition.getArchetypeDetails().getTemplateId().getValue();
      FlatJson flattener =
          new FlatJasonProvider(new TestDataTemplateProvider())
              .buildFlatJson(FlatFormat.SIM_SDT, templateId);
      return mapper.readValue(flattener.marshal(composition), Map.class);
    } catch (JsonProcessingException e) {
      throw new UnsupportedOperationException("Cannot parse results");
    } catch (SdkException e){
      throw new SystemException(e.getMessage());
    }
  }

  private void validateComposition(Composition composition) {
    if (composition.getArchetypeDetails() == null
        || composition.getArchetypeDetails().getTemplateId() == null
        || composition.getArchetypeDetails().getTemplateId().getValue() == null) {
      throw new UnsupportedOperationException(
          "Cannot parse results, composition missing template id");
    }
  }
}
