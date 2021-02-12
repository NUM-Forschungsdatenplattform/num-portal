package de.vitagroup.num.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.PhenotypeRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.executors.PhenotypeExecutor;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.PrivacyException;
import java.util.HashSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PhenotypeServiceTest {

  @InjectMocks private PhenotypeService phenotypeService;

  @Mock private PhenotypeRepository phenotypeRepository;

  @Mock private UserDetailsService userDetailsService;

  @Mock private AqlService aqlService;

  @Mock private PrivacyProperties privacyProperties;

  @Mock private PhenotypeExecutor phenotypeExecutor;

  @Before
  public void setup() {
    UserDetails approvedUser =
        UserDetails.builder().userId("approvedUserId").approved(true).build();

    UserDetails notApprovedUser =
        UserDetails.builder().userId("notApprovedUserId").approved(false).build();

    when(userDetailsService.getUserDetailsById("approvedUserId"))
        .thenReturn(Optional.of(approvedUser));

    when(userDetailsService.getUserDetailsById("notApprovedUserId"))
        .thenReturn(Optional.of(notApprovedUser));

    when(aqlService.getAqlById(1L))
        .thenReturn(
            Optional.of(
                Aql.builder()
                    .id(1L)
                    .owner(UserDetails.builder().userId("approvedUserId").approved(true).build())
                    .build()));

    when(aqlService.getAqlById(2L)).thenReturn(Optional.empty());
    when(phenotypeExecutor.execute(any(Phenotype.class))).thenReturn(new HashSet<>());
  }

  @Test
  public void shouldCallRepoWhenRetrievingAllPhenotypes() {
    phenotypeService.getAllPhenotypes("approvedUserId");
    verify(phenotypeRepository, times(1)).findByOwnerUserId("approvedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailWhenRetrievingAllPhenotypesUnapproved() {
    phenotypeService.getAllPhenotypes("notApprovedUserId");
  }

  @Test
  public void shouldCallRepoWhenCreatingPhenotype() {

    AqlExpression query = AqlExpression.builder().aql(Aql.builder().id(1L).build()).build();

    phenotypeService.createPhenotypes(Phenotype.builder().query(query).build(), "approvedUserId");

    verify(phenotypeRepository, times(1)).save(any());
  }

  @Test(expected = BadRequestException.class)
  public void shouldValidateWhenCreatingPhenotype() {
    AqlExpression query = AqlExpression.builder().aql(Aql.builder().id(2L).build()).build();
    phenotypeService.createPhenotypes(Phenotype.builder().query(query).build(), "approvedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailWhenCreatingPhenotypeUnapproved() {
    AqlExpression query = AqlExpression.builder().aql(Aql.builder().id(2L).build()).build();
    phenotypeService.createPhenotypes(
        Phenotype.builder().query(query).build(), "notApprovedUserId");
  }

  @Test
  public void shouldCallEhrBaseWhenGettingPhenotypeSize() {

    AqlExpression query = AqlExpression.builder().aql(Aql.builder().id(1L).build()).build();

    phenotypeService.getPhenotypeSize(Phenotype.builder().query(query).build(), "approvedUserId");

    verify(phenotypeExecutor, times(1)).execute(any());
  }

  @Test(expected = PrivacyException.class)
  public void shouldThrowPrivacyExceptionWhenGettingPhenotypeSize() {
    when(privacyProperties.getMinHits()).thenReturn(1);
    AqlExpression query = AqlExpression.builder().aql(Aql.builder().id(1L).build()).build();

    phenotypeService.getPhenotypeSize(Phenotype.builder().query(query).build(), "approvedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailWhenWhenGettingPhenotypeSizeUnapproved() {
    phenotypeService.getPhenotypeSize(null, "notApprovedUserId");
  }
}
