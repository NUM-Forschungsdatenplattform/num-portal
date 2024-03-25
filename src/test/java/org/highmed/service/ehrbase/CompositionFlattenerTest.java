package org.highmed.service.ehrbase;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.composition.Composition;
import lombok.SneakyThrows;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.openehr.sdk.client.templateprovider.ClientTemplateProvider;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openehr.schemas.v1.TemplateDocument;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.highmed.service.exception.SystemException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.highmed.domain.templates.ExceptionsTemplate.CANNOT_PARSE_RESULTS_COMPOSITION_MISSING_TEMPLATE_ID;

@RunWith(MockitoJUnitRunner.class)
public class CompositionFlattenerTest {

  @Mock
  ClientTemplateProvider clientTemplateProvider;

  @InjectMocks private CompositionFlattener flattener;

  private final String CORONA_PATH = "/testdata/corona.json";
  private final String CORONA_OTP = "/testdata/corona_anamnese.opt";

  @Before
  public void setup() throws IOException, XmlException {
    flattener.clearCaches();
    flattener.initializeTemplateCache();

    when(clientTemplateProvider.find(anyString()))
        .thenReturn(
            Optional.of(
                TemplateDocument.Factory.parse(getClass().getResourceAsStream(CORONA_OTP))
                    .getTemplate()));
  }

  @After
  public void clear() {
    flattener.clearCaches();
  }

  @Test
  @SneakyThrows
  public void shouldFlattenComposition() {

    Composition composition =
        new CanonicalJson()
            .unmarshal(
                IOUtils.toString(
                    getClass().getResourceAsStream(CORONA_PATH), StandardCharsets.UTF_8),
                Composition.class);

    assertThat(composition, notNullValue());

    Map<String, Object> values = flattener.flatten(composition);

    assertThat(values, notNullValue());

    assertEquals(
        "Heiserkeit",
        values.get(
            "bericht/symptome/heiserkeit/spezifisches_symptom_anzeichen/bezeichnung_des_symptoms_oder_anzeichens."));
    assertEquals(
        "Ja",
        values.get(
            "bericht/risikogebiet/historie_der_reise:0/aufenthalt_in_den_letzten_14_tage_in_einem_der_risikogebiete_für_coronainfektion_oder_kontakt_zu_menschen_die_dort_waren|value"));
    assertEquals(
        "Norditalien",
        values.get(
            "bericht/risikogebiet/historie_der_reise:0/ortsbeschreibung/standort/standortbeschreibung"));
    assertEquals(
        "Baden-Württemberg",
        values.get(
            "bericht/risikogebiet/reisefall:0/beliebiges_intervallereignis:0/bestimmte_reise:0/bestimmtes_reiseziel:0/bundesland_region"));
  }

  @Test(expected = SystemException.class)
  @SneakyThrows
  public void shouldHandleMissingTemplateId() {
    Composition composition =
        new CanonicalJson()
            .unmarshal(
                IOUtils.toString(
                    getClass().getResourceAsStream(CORONA_PATH), StandardCharsets.UTF_8),
                Composition.class);

    assertThat(composition, notNullValue());

    composition.getArchetypeDetails().setTemplateId(null);

    flattener.flatten(composition);
  }

  @Test(expected = SystemException.class)
  public void validateCompositionSystemException() {
    Composition composition = new Composition();
    when(flattener.flatten(composition))
            .thenThrow(new SystemException(CompositionFlattener.class, CANNOT_PARSE_RESULTS_COMPOSITION_MISSING_TEMPLATE_ID));

    flattener.flatten(composition);
  }

  @Test(expected = SystemException.class)
  public void flattenSystemException() {
    Composition composition = new Composition();
    Archetyped archetyped = new Archetyped();
    composition.setArchetypeDetails(archetyped);

    flattener.flatten(composition);
  }
}
