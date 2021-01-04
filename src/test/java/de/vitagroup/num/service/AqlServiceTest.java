package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.NotAuthorizedException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.EmptyResultDataAccessException;

@RunWith(MockitoJUnitRunner.class)
public class AqlServiceTest {

  @Mock private AqlRepository aqlRepository;

  @Mock private UserDetailsRepository userDetailsRepository;

  @InjectMocks private AqlService aqlService;

  @Before
  public void setup() {
    UserDetails approvedUser =
        UserDetails.builder().userId("approvedUserId").approved(true).build();

    when(userDetailsRepository.findByUserId("notApprovedId"))
        .thenReturn(
            Optional.of(UserDetails.builder().userId("notApprovedId").approved(false).build()));

    when(userDetailsRepository.findByUserId("approvedUserId"))
        .thenReturn(Optional.of(approvedUser));

    when(aqlRepository.findById(1L))
        .thenReturn(
            Optional.ofNullable(
                Aql.builder()
                    .id(1L)
                    .name("name to edit")
                    .description("description to edit")
                    .createDate(OffsetDateTime.now().minusDays(4))
                    .createDate(OffsetDateTime.now())
                    .publicAql(true)
                    .organizationId("1234")
                    .owner(approvedUser)
                    .build()));

    when(aqlRepository.findById(2L))
        .thenReturn(Optional.ofNullable(Aql.builder().owner(null).build()));
    when(aqlRepository.findById(3L))
        .thenReturn(Optional.ofNullable(Aql.builder().owner(approvedUser).build()));

    when(aqlRepository.save(any())).thenReturn(createAql(OffsetDateTime.now()));

    doThrow(EmptyResultDataAccessException.class).when(aqlRepository).deleteById(3L);
  }

  @Test
  public void shouldSuccessfullyCreateAql() {
    Aql toSave = createAql(OffsetDateTime.now());
    Aql createdAql = aqlService.createAql(toSave, "approvedUserId");

    assertThat(createdAql, notNullValue());
    assertThat(createdAql.getName(), is(toSave.getName()));
    assertThat(createdAql.getDescription(), is(toSave.getDescription()));
    assertThat(createdAql.isPublicAql(), is(toSave.isPublicAql()));
  }

  @Test
  public void shouldSuccessfullyEditAql() {
    Aql toEdit = createAql(OffsetDateTime.now());
    Aql updatedAql = aqlService.updateAql(toEdit, 1L, "approvedUserId");

    assertThat(updatedAql, notNullValue());
    assertThat(updatedAql.getName(), is(toEdit.getName()));
    assertThat(updatedAql.getDescription(), is(toEdit.getDescription()));
    assertThat(updatedAql.isPublicAql(), is(toEdit.isPublicAql()));
    assertThat(updatedAql.getOrganizationId(), is(toEdit.getOrganizationId()));
  }

  @Test(expected = NotAuthorizedException.class)
  public void shouldHandleMissingAqlOwnerWhenEditing() {
    Aql toEdit = createAql(OffsetDateTime.now());
    aqlService.updateAql(toEdit, 2L, "approvedUserId");
  }

  @Test(expected = NotAuthorizedException.class)
  public void shouldHandleMissingOwner() {
    aqlService.createAql(Aql.builder().build(), "missingOwnerId");
  }

  @Test(expected = NotAuthorizedException.class)
  public void shouldHandleNotApprovedOwner() {
    aqlService.createAql(Aql.builder().build(), "notApprovedId");
  }

  @Test
  public void shouldCallRepoWhenRetrievingAllAqls() {
    aqlService.getAllAqls();
    verify(aqlRepository, times(1)).findAll();
  }

  @Test
  public void shouldCallRepoWhenSearchingAql() {
    aqlService.getAqlById(any());
    verify(aqlRepository, times(1)).findById(any());
  }

  @Test
  public void shouldCallRepoWhenDeleting() {
    aqlService.deleteById(1L, "approvedUserId");
    verify(aqlRepository, times(1)).deleteById(1L);
  }

  @Test(expected = NotAuthorizedException.class)
  public void shouldHandleMissingOwnerWhenDeleting() {
    aqlService.deleteById(2L, "approvedUserId");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingAqlOwnerWhenDeleting() {
    aqlService.deleteById(3L, "approvedUserId");
  }

  @Test(expected = NotAuthorizedException.class)
  public void shouldHandleNonExistingUser() {
    aqlService.deleteById(1L, "nonExistingUser");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleNonExistingAql() {
    aqlService.deleteById(9L, "approvedUserId");
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldCallRepoWhenSearching() {
    aqlService.deleteById(9L, "approvedUserId");
  }

  @Test
  public void shouldSearchAllWhenNoSearchParameters() {
    aqlService.searchAqls(null, null, null, "approvedUserId");
    verify(aqlRepository, times(1)).findAll();
  }

  @Test
  public void shouldCriteriaSearchAllAtLeastOneSearchParameterPresent() {
    aqlService.searchAqls("for name", null, null, "approvedUserId");
    verify(aqlRepository, times(1)).findAqlByNameAndOrganizationAndOwner("for name", null, null);
  }

  private Aql createAql(OffsetDateTime createdAndModifiedDate) {
    return Aql.builder()
        .id(10L)
        .name("name")
        .description("description")
        .publicAql(false)
        .createDate(createdAndModifiedDate)
        .modifiedDate(createdAndModifiedDate)
        .organizationId("abc")
        .build();
  }
}
