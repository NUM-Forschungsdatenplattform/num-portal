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
import de.vitagroup.num.domain.AqlCategory;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.AqlCategoryRepository;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Mock private AqlCategoryRepository aqlCategoryRepository;

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks private AqlService aqlService;

  @Before
  public void setup() {
    UserDetails approvedUser =
        UserDetails.builder().userId("approvedUserId").approved(true).build();

    when(userDetailsService.checkIsUserApproved("notApprovedId"))
        .thenThrow(new ForbiddenException("Cannot access this resource. User is not approved."));

    when(userDetailsService.checkIsUserApproved("nonExistingUser"))
        .thenThrow(new SystemException("User not found"));

    when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);

    when(aqlRepository.findById(1L))
        .thenReturn(
            Optional.ofNullable(
                Aql.builder()
                    .id(1L)
                    .name("name to edit")
                    .use("use to edit")
                    .purpose("purpose to edit")
                    .createDate(OffsetDateTime.now().minusDays(4))
                    .createDate(OffsetDateTime.now())
                    .publicAql(true)
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
    assertThat(createdAql.getUse(), is(toSave.getUse()));
    assertThat(createdAql.getPurpose(), is(toSave.getPurpose()));
    assertThat(createdAql.isPublicAql(), is(toSave.isPublicAql()));
  }

  @Test
  public void shouldSuccessfullyEditAql() {
    Aql toEdit = createAql(OffsetDateTime.now());
    Aql updatedAql = aqlService.updateAql(toEdit, 1L, "approvedUserId");

    assertThat(updatedAql, notNullValue());
    assertThat(updatedAql.getName(), is(toEdit.getName()));
    assertThat(updatedAql.getUse(), is(toEdit.getUse()));
    assertThat(updatedAql.getPurpose(), is(toEdit.getPurpose()));
    assertThat(updatedAql.isPublicAql(), is(toEdit.isPublicAql()));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleMissingAqlOwnerWhenEditing() {
    Aql toEdit = createAql(OffsetDateTime.now());
    aqlService.updateAql(toEdit, 2L, "approvedUserId");
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingOwner() {
    aqlService.createAql(Aql.builder().build(), "nonExistingUser");
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedOwner() {
    aqlService.createAql(Aql.builder().build(), "notApprovedId");
  }

  @Test
  public void shouldCallRepoWhenSearchingAql() {
    aqlService.getAqlById(1L, "approvedUserId");
    verify(aqlRepository, times(1)).findById(any());
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailWhenDeletingIfCoordinator() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.STUDY_COORDINATOR));
    verify(aqlRepository, times(1)).deleteById(1L);
  }

  @Test
  public void shouldCallRepoWhenDeletingIfManager() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.MANAGER));
    verify(aqlRepository, times(1)).deleteById(1L);
  }

  @Test
  public void shouldCallRepoWhenDeletingIfSuperAdmin() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.SUPER_ADMIN));
    verify(aqlRepository, times(1)).deleteById(1L);
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleMissingOwnerWhenDeleting() {
    aqlService.deleteById(2L, "approvedUserId",  List.of(Roles.MANAGER));
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingAqlOwnerWhenDeleting() {
    aqlService.deleteById(3L, "approvedUserId",  List.of(Roles.MANAGER));
  }

  @Test(expected = SystemException.class)
  public void shouldHandleNonExistingUser() {
    aqlService.deleteById(1L, "nonExistingUser",  List.of());
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleNonExistingAql() {
    aqlService.deleteById(9L, "approvedUserId",  List.of());
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldCallRepoWhenSearching() {
    aqlService.deleteById(9L, "approvedUserId",  List.of());
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleNotExistingCategoryWhenDeleting() {
    when(aqlRepository.findByCategoryId(1L)).thenReturn(List.of());
    when(aqlCategoryRepository.existsById(1L)).thenReturn(false);
    aqlService.deleteCategoryById(1L);
    verify(aqlCategoryRepository, times(0)).deleteById(1L);
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleCategoryInUseWhenDeleting() {
    when(aqlRepository.findByCategoryId(1L)).thenReturn(List.of(new Aql()));
    aqlService.deleteCategoryById(1L);
    verify(aqlCategoryRepository, times(0)).deleteById(1L);
  }

  @Test
  public void shouldDeleteCategory() {
    when(aqlRepository.findByCategoryId(1L)).thenReturn(List.of());
    when(aqlCategoryRepository.existsById(1L)).thenReturn(true);
    aqlService.deleteCategoryById(1L);
    verify(aqlCategoryRepository, times(1)).deleteById(1L);
  }

  @Test
  public void shouldCreateValidCategory() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).build();
    aqlService.createAqlCategory(category);
    verify(aqlCategoryRepository, times(1)).save(category);
  }

  @Test
  public void shouldUpdateValidCategory() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).id(1L).build();
    when(aqlCategoryRepository.existsById(1L)).thenReturn(true);
    aqlService.updateAqlCategory(category, 1L);
    verify(aqlCategoryRepository, times(1)).save(category);
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldNotUpdateNotExistingCategory() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).id(1L).build();
    when(aqlCategoryRepository.existsById(1L)).thenReturn(false);
    aqlService.updateAqlCategory(category, 1L);
    verify(aqlCategoryRepository, times(0)).save(category);
  }

  @Test(expected = BadRequestException.class)
  public void shouldNotUpdateCategoryWithoutId() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).id(1L).build();
    aqlService.updateAqlCategory(category, null);
    verify(aqlCategoryRepository, times(0)).save(category);
  }

  @Test
  public void shouldGetAllCategories() {
    aqlService.getAqlCategories();
    verify(aqlCategoryRepository, times(1)).findAll();
  }


  private Aql createAql(OffsetDateTime createdAndModifiedDate) {
    return Aql.builder()
        .id(10L)
        .name("name")
        .use("use")
        .purpose("purpose")
        .publicAql(false)
        .createDate(createdAndModifiedDate)
        .modifiedDate(createdAndModifiedDate)
        .build();
  }
}
