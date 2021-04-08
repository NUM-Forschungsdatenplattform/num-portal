package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.CohortGroupRepository;
import de.vitagroup.num.domain.repository.PhenotypeRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.executors.PhenotypeExecutor;
import de.vitagroup.num.web.config.Role;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.PrivacyException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PhenotypeServiceTest {

  @InjectMocks private PhenotypeService phenotypeService;

  @Mock private PhenotypeRepository phenotypeRepository;

  @Mock private CohortGroupRepository cohortGroupRepository;

  @Mock private UserDetailsService userDetailsService;

  @Mock private AqlService aqlService;

  @Mock private PrivacyProperties privacyProperties;

  @Mock private PhenotypeExecutor phenotypeExecutor;

  @Captor ArgumentCaptor<Phenotype> phenotypeArgumentCaptor;

  @Test
  public void shouldHardDeleteOwnedNotUsedPhenotype() {
    when(phenotypeRepository.findById(2L))
        .thenReturn(
            Optional.of(
                Phenotype.builder()
                    .id(2L)
                    .owner(UserDetails.builder().userId("approvedUserId").build())
                    .build()));
    when(phenotypeRepository.existsById(2L)).thenReturn(true);
    when(cohortGroupRepository.existsByPhenotypeId(2L)).thenReturn(false);

    phenotypeService.deletePhenotypeById(2L, "approvedUserId", List.of(Role.STUDY_COORDINATOR));

    verify(phenotypeRepository, times(1)).deleteById(2L);
  }

  @Test
  public void shouldHardDeleteNotOwnedNotUsedPhenotypeIfAdmin() {
    when(phenotypeRepository.findById(2L))
        .thenReturn(
            Optional.of(
                Phenotype.builder()
                    .id(2L)
                    .owner(UserDetails.builder().userId("someOtherOwnerId").build())
                    .build()));

    when(phenotypeRepository.existsById(2L)).thenReturn(true);
    when(cohortGroupRepository.existsByPhenotypeId(2L)).thenReturn(false);

    phenotypeService.deletePhenotypeById(2L, "approvedUserId", List.of(Roles.SUPER_ADMIN));

    verify(phenotypeRepository, times(1)).deleteById(2L);
    verify(phenotypeRepository, times(0)).save(any());
  }

  @Test
  public void shouldSoftDeleteOwnedUsedPhenotype() {
    Phenotype phenotype =
        Phenotype.builder()
            .id(3L)
            .owner(UserDetails.builder().userId("approvedUserId").build())
            .build();
    when(phenotypeRepository.findById(3L)).thenReturn(Optional.of(phenotype));
    when(phenotypeRepository.existsById(3L)).thenReturn(true);
    when(cohortGroupRepository.existsByPhenotypeId(3L)).thenReturn(true);

    phenotypeService.deletePhenotypeById(3L, "approvedUserId", List.of(Role.STUDY_COORDINATOR));

    verify(phenotypeRepository, times(0)).deleteById(3L);
    verify(phenotypeRepository, times(1)).save(phenotype);
  }

  @Test
  public void shouldCallRepoWhenRetrievingAllPhenotypes() {
    phenotypeService.getAllPhenotypes("approvedUserId");
    verify(phenotypeRepository, times(0)).findAll();
    verify(phenotypeRepository, times(1)).findByDeletedFalse();
  }

  @Test
  public void shouldCallRepoWhenRetrievingPhenotypeById() {
    phenotypeService.getPhenotypeById(1L, "approvedUserId");
    verify(phenotypeRepository, times(1)).findById(1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandlePhenotypeNotFound() {
    phenotypeService.getPhenotypeById(1324L, "approvedUserId");
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

  @Test
  public void shouldCorrectlyCreatePhenotype() {
    AqlExpression aqlExpression = AqlExpression.builder().aql(Aql.builder().id(1L).build()).build();

    Phenotype phenotype =
        Phenotype.builder()
            .name("Phenotype to create")
            .description("Phenotype description")
            .query(aqlExpression)
            .build();

    phenotypeService.createPhenotypes(phenotype, "approvedUserId");
    Mockito.verify(phenotypeRepository).save(phenotypeArgumentCaptor.capture());

    Phenotype phenotypeToSave = phenotypeArgumentCaptor.getValue();

    assertThat(phenotypeToSave, notNullValue());
    assertThat(phenotypeToSave.getName(), is("Phenotype to create"));
    assertThat(phenotypeToSave.getDescription(), is("Phenotype description"));
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleNotOwnedAql() {
    AqlExpression aqlExpression = AqlExpression.builder().aql(Aql.builder().id(3L).build()).build();

    Phenotype phenotype =
        Phenotype.builder()
            .name("Phenotype to create")
            .description("Phenotype description")
            .query(aqlExpression)
            .build();

    phenotypeService.createPhenotypes(phenotype, "approvedUserId");
  }

  @Test
  public void shouldAllowNotOwnedPublicAql() {
    AqlExpression aqlExpression = AqlExpression.builder().aql(Aql.builder().id(4L).build()).build();

    Phenotype phenotype =
        Phenotype.builder()
            .name("Phenotype to create 2")
            .description("Phenotype description 2")
            .query(aqlExpression)
            .build();

    phenotypeService.createPhenotypes(phenotype, "approvedUserId");
    Mockito.verify(phenotypeRepository).save(phenotypeArgumentCaptor.capture());

    Phenotype phenotypeToSave = phenotypeArgumentCaptor.getValue();

    assertThat(phenotypeToSave, notNullValue());
    assertThat(phenotypeToSave.getName(), is("Phenotype to create 2"));
    assertThat(phenotypeToSave.getDescription(), is("Phenotype description 2"));
  }

  @Before
  public void setup() {
    UserDetails approvedUser =
        UserDetails.builder().userId("approvedUserId").approved(true).build();

    when(userDetailsService.checkIsUserApproved("notApprovedUserId"))
        .thenThrow(new ForbiddenException("Cannot access this resource. User is not approved."));

    when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);

    when(aqlService.getAqlById(1L))
        .thenReturn(Optional.of(Aql.builder().id(1L).publicAql(false).owner(approvedUser).build()));

    when(phenotypeRepository.findById(1L))
        .thenReturn(Optional.of(Phenotype.builder().id(1L).build()));

    when(aqlService.getAqlById(3L))
        .thenReturn(
            Optional.of(
                Aql.builder()
                    .id(1L)
                    .publicAql(false)
                    .owner(UserDetails.builder().userId("someOtherUserId").approved(true).build())
                    .build()));

    when(aqlService.getAqlById(4L))
        .thenReturn(
            Optional.of(
                Aql.builder()
                    .id(1L)
                    .publicAql(true)
                    .owner(UserDetails.builder().userId("someOtherUserId").approved(true).build())
                    .build()));

    when(aqlService.getAqlById(2L)).thenReturn(Optional.empty());
    when(phenotypeExecutor.execute(any(Phenotype.class))).thenReturn(new HashSet<>());
  }
}
