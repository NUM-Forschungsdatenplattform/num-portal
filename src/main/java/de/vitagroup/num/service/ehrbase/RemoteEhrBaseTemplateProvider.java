package de.vitagroup.num.service.ehrbase;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ehrbase.client.openehrclient.defaultrestclient.DefaultRestClient;
import org.ehrbase.webtemplate.templateprovider.TemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemoteEhrBaseTemplateProvider implements TemplateProvider {

  private final DefaultRestClient defaultRestClient;

  @Override
  public Optional<OPERATIONALTEMPLATE> find(String templateId) {
    return  defaultRestClient.templateEndpoint().findTemplate(templateId);
  }
}
