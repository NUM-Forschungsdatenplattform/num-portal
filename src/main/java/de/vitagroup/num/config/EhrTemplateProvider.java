package de.vitagroup.num.config;

import java.util.Optional;
import org.ehrbase.client.templateprovider.TemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public class EhrTemplateProvider implements TemplateProvider {

    @Override
    public Optional<OPERATIONALTEMPLATE> find(String templateId) {
        return Optional.empty();
    }
}
