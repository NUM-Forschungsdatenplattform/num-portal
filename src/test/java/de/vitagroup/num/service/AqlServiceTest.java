package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.NotAuthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AqlServiceTest {

  @Mock private AqlRepository aqlRepository;

  @Mock private UserDetailsRepository userDetailsRepository;

  @InjectMocks private AqlService aqlService;

  @Before
  public void setup() {
    when(userDetailsRepository.findByUserId("notApprovedId"))
        .thenReturn(
            Optional.of(UserDetails.builder().userId("notApprovedId").approved(false).build()));

    when(userDetailsRepository.findByUserId("approvedUserId"))
        .thenReturn(
            Optional.of(UserDetails.builder().userId("approvedUserId").approved(true).build()));

    when(aqlRepository.save(any())).thenReturn(createAql());
  }

  @Test
  public void shouldSuccessfullyCreateAql() {
    Aql toSave = createAql();
    Aql createdAql = aqlService.createAql(toSave, "approvedUserId");

    assertThat(createdAql, notNullValue());
    assertThat(createdAql.getName(), is(toSave.getName()));
    assertThat(createdAql.getDescription(), is(toSave.getDescription()));
    assertThat(createdAql.isPublicAql(), is(toSave.isPublicAql()));
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

  private Aql createAql() {
    return Aql.builder()
        .name("name")
        .description("description")
        .publicAql(false)
        .createDate(OffsetDateTime.now())
        .modifiedDate(OffsetDateTime.now())
        .organizationId("abc")
        .build();
  }
}
