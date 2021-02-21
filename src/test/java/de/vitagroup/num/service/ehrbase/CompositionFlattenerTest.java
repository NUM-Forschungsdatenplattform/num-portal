package de.vitagroup.num.service.ehrbase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nedap.archie.rm.composition.Composition;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.SneakyThrows;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@RunWith(SpringRunner.class)
@Import(CompositionFlattener.class)
public class CompositionFlattenerTest {

  @Autowired
  private CompositionFlattener flattener;

  private final String CORONA_PATH = "/testdata/corona.json";

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

    Map<String, String> values = flattener.flatten(composition);

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

  @Test(expected = UnsupportedOperationException.class)
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
}
