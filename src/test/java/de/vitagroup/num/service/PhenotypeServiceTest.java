package de.vitagroup.num.service;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.repository.PhenotypeRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PhenotypeServiceTest {

  @InjectMocks private PhenotypeService phenotypeService;

  @Mock private PhenotypeRepository phenotypeRepository;

  @Test
  public void shouldHandleMissingPhenotype() {
    Optional<Phenotype> phenotype = phenotypeService.getPhenotypeById(1L);

    assertThat(phenotype, notNullValue());
    assertThat(phenotype.isEmpty(), is(true));
  }

  @Test
  public void shouldCallRepoWhenRetrievingAllPhenotypes() {
    phenotypeService.getAllPhenotypes();
    verify(phenotypeRepository, times(1)).findAll();
  }

  @Test
  public void shouldCallRepoWhenCreatingPhenotype() {
    phenotypeService.createPhenotypes(Phenotype.builder().build());
    verify(phenotypeRepository, times(1)).save(any());
  }
}
