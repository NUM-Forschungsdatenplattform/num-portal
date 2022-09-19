package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlCategory;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.AqlSearchFilter;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.domain.repository.AqlCategoryRepository;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.specification.AqlSpecification;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.exception.SystemException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.JpaSort;

import java.time.OffsetDateTime;
import java.util.*;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.*;

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
        .thenThrow(new ForbiddenException(AqlServiceTest.class, CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED));

    when(userDetailsService.checkIsUserApproved("nonExistingUser"))
        .thenThrow(new SystemException(AqlServiceTest.class, USER_NOT_FOUND));

    when(userDetailsService.checkIsUserApproved("approvedUserId")).thenReturn(approvedUser);

    Organization orgA = Organization.builder()
            .id(1L)
            .name("aa some organization name")
            .build();
    UserDetails criteriaEditor = UserDetails.builder()
            .userId("approvedCriteriaEditorId")
            .approved(true)
            .organization(orgA)
            .build();
    Mockito.when(userDetailsService.checkIsUserApproved("approvedCriteriaEditorId")).thenReturn(criteriaEditor);

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
  public void getAqlByIdTest() {
    aqlService.getAqlById(1L, "approvedUserId");
    Mockito.verify(aqlRepository, Mockito.times(1)).findById(Mockito.eq(1L));
  }

  @Test
  public void searchAqlsTest() {
    String searchInput = "dummy search";
      aqlService.searchAqls(searchInput, AqlSearchFilter.OWNED, "approvedUserId");
      Mockito.verify(aqlRepository, Mockito.times(1)).findAllOwnedByName(Mockito.eq("approvedUserId"), Mockito.eq(searchInput.toUpperCase()));
  }

  @Test
  public void shouldSuccessfullyCreateAql() {
    Aql toSave = createAql(OffsetDateTime.now());
    Aql createdAql = aqlService.createAql(toSave, "approvedUserId");

    assertThat(createdAql, notNullValue());
    assertThat(createdAql.getName(), is(toSave.getName()));
    assertThat(createdAql.getUse(), is(toSave.getUse()));
    assertThat(createdAql.getPurpose(), is(toSave.getPurpose()));
    assertThat(createdAql.getNameTranslated(), is(toSave.getNameTranslated()));
    assertThat(createdAql.getUseTranslated(), is(toSave.getUseTranslated()));
    assertThat(createdAql.getPurposeTranslated(), is(toSave.getPurposeTranslated()));
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

  @Test(expected = ForbiddenException.class)
  public void shouldFailWhenDeletingIfManager() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.MANAGER));
    verify(aqlRepository, Mockito.never()).deleteById(1L);
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

  @Test(expected = ResourceNotFound.class)
  public void getAqlById() {
    aqlService.getAqlById(1000L, "approvedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void getAqlByIdForbiddenException() {
    aqlService.getAqlById(2L, "approvedUserId");
  }

  @Test(expected = ResourceNotFound.class)
  public void updateAql() {
    aqlService.updateAql(new Aql(), 1000L, "approvedUserId");
  }

  @Test(expected = ForbiddenException.class)
  public void updateAqlForbiddenException() {
    aqlService.updateAql(new Aql(), 2L, "approvedUserId");
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteById() {
    aqlService.deleteById(1000L, "approvedUserId", List.of(Roles.STUDY_COORDINATOR));
  }

  @Test(expected = ForbiddenException.class)
  public void deleteByIdForbiddenException() {
    aqlService.deleteById(1L, "approvedUserId", List.of());
  }

  @Test(expected = BadRequestException.class)
  public void updateAqlCategory() {
    aqlService.updateAqlCategory(new AqlCategory(),null);
  }

  @Test(expected = ResourceNotFound.class)
  public void updateAqlCategoryResourceNotFound() {
    aqlService.updateAqlCategory(new AqlCategory(),1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteCategoryById() {
    aqlService.deleteCategoryById(null);
  }

  @Test(expected = BadRequestException.class)
  public void deleteCategoryByIdBadRequestException() {
    when(aqlRepository.findByCategoryId(null)).thenReturn(Arrays.asList(new Aql()));
    aqlService.deleteCategoryById(null);
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailWhenDeletingIfCoordinator() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.STUDY_COORDINATOR));
    verify(aqlRepository, Mockito.never()).deleteById(1L);
  }

  @Test
  public void shouldCallRepoWhenDeletingIfCriteriaEditor() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.CRITERIA_EDITOR));
    verify(aqlRepository, times(1)).deleteById(1L);
  }

  @Test
  public void shouldCallRepoWhenDeletingIfSuperAdmin() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.SUPER_ADMIN));
    verify(aqlRepository, times(1)).deleteById(1L);
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleMissingOwnerWhenDeleting() {
    aqlService.deleteById(2L, "approvedUserId",  List.of(Roles.CRITERIA_EDITOR));
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingAqlOwnerWhenDeleting() {
    aqlService.deleteById(3L, "approvedUserId",  List.of(Roles.CRITERIA_EDITOR));
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
    verify(aqlCategoryRepository, times(1)).findAllCategories();
  }

  @Test
  public void getAqlCategoriesWithPaginationTest() {
    Pageable pageable = PageRequest.of(0,30).withSort(JpaSort.unsafe(Sort.Direction.ASC, "name->>'de'"));;
    aqlService.getAqlCategories(pageable, new SearchCriteria());
    verify(aqlCategoryRepository, times(1)).findAllCategories(Mockito.eq(pageable));
  }

  @Test
  public void getAqlCategoriesWithPaginationAndSortByGermanNameTest() {
    Pageable pageableWithoutSort = PageRequest.of(0,30);
    aqlService.getAqlCategories(pageableWithoutSort, SearchCriteria.builder()
            .sortBy("name-de")
            .sort("ASC").build());
    Pageable pageableWithSort = PageRequest.of(0,30).withSort(JpaSort.unsafe(Sort.Direction.ASC, "name->>'de'"));
    ArgumentCaptor<Pageable> pageableWithSortCapture = ArgumentCaptor.forClass(Pageable.class);
    verify(aqlCategoryRepository, times(1)).findAllCategories(pageableWithSortCapture.capture());
    Pageable capturedInput = pageableWithSortCapture.getValue();
    Assert.assertEquals(pageableWithSort, capturedInput);
  }

  @Test
  public void getAqlCategoriesWithPaginationAndSortByEnglishNameTest() {
    Pageable pageableWithourSort = PageRequest.of(0,30);
    aqlService.getAqlCategories(pageableWithourSort, SearchCriteria.builder()
            .sortBy("name-en")
            .sort("desc").build());
    Pageable pageableWithSort = PageRequest.of(0,30).withSort(JpaSort.unsafe(Sort.Direction.DESC, "name->>'en'"));
    ArgumentCaptor<Pageable> pageableWithSortCapture = ArgumentCaptor.forClass(Pageable.class);
    verify(aqlCategoryRepository, times(1)).findAllCategories(pageableWithSortCapture.capture());
    Pageable capturedInput = pageableWithSortCapture.getValue();
    Assert.assertEquals(pageableWithSort, capturedInput);
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingSortFieldWhenGetAqlCategories() {
    Pageable pageable = PageRequest.of(0,50);
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sortBy("ASC")
            .build();
    aqlService.getAqlCategories(pageable, searchCriteria);
    verify(aqlCategoryRepository, never());
  }

  @Test
  public void countAqlsTest() {
    aqlService.countAqls();
    Mockito.verify(aqlRepository, Mockito.times(1)).count();
  }

  @Test
  public void getVisibleAqlsTest() {
    aqlService.getVisibleAqls("approvedUserId");
    Mockito.verify(aqlRepository, Mockito.times(1)).findAllOwnedOrPublic("approvedUserId");
  }

  @Test
  public void getVisibleAqlsWithPaginationAndSortByName() {
    Pageable pageRequest = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "name"));
    Aql aqlOne = Aql.builder()
            .id(1L)
            .name("aql test one")
            .publicAql(true)
            .query("select * from dummy_table")
            .build();
    Aql aqlTwo = Aql.builder()
            .id(2L)
            .name("aql test two")
            .publicAql(true)
            .query("select * from dummy_table where dummy column = ")
            .build();
    Mockito.when(aqlRepository.findAll(Mockito.any(AqlSpecification.class), Mockito.any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(aqlOne, aqlTwo)));
    ArgumentCaptor<AqlSpecification> specArgumentCaptor = ArgumentCaptor.forClass(AqlSpecification.class);
    ArgumentCaptor<Pageable> pageableCapture = ArgumentCaptor.forClass(Pageable.class);
    aqlService.getVisibleAqls("approvedCriteriaEditorId", pageRequest, SearchCriteria.builder()
            .sortBy("name")
            .sort("asc")
            .build());
    Mockito.verify(aqlRepository, Mockito.times(1)).findAll(specArgumentCaptor.capture(), pageableCapture.capture());
    Pageable capturedInput = pageableCapture.getValue();
    Assert.assertEquals(pageRequest, capturedInput);
    AqlSpecification aqlSpecification = specArgumentCaptor.getValue();
    Assert.assertEquals("approvedCriteriaEditorId", aqlSpecification.getLoggedInUserId());
    Assert.assertEquals("de", aqlSpecification.getLanguage());
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleInvalidSortTest() {
    Pageable pageable = PageRequest.of(0,50);
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sort("dummyName")
            .sortBy("ASC")
            .build();
    aqlService.getVisibleAqls("approvedCriteriaEditorId", pageable, searchCriteria);
    verify(aqlRepository, never());
  }

  @Test
  public void getVisibleAqlsWithPaginationAndSortByCategoryName() {
    Pageable pageRequest = PageRequest.of(0, 100);
    Map<String, String> tr1 = new HashMap<>();
    tr1.put("en", "category one in english");
    tr1.put("de", "category one in german");
    Aql aqlOne = Aql.builder()
            .id(1L)
            .name("aql test one")
            .publicAql(true)
            .query("select * from dummy_table")
            .category(AqlCategory.builder().id(1L).name(tr1).build())
            .build();
    Map<String, String> tr2 = new HashMap<>();
    tr2.put("en", "category two in english");
    tr2.put("de", "category two in german");
    Aql aqlTwo = Aql.builder()
            .id(2L)
            .name("aql test two")
            .publicAql(true)
            .query("select * from dummy_table where dummy column = ")
            .category(AqlCategory.builder().id(2L).name(tr2).build())
            .build();
    Mockito.when(aqlRepository.findAll(Mockito.any(AqlSpecification.class), Mockito.any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(aqlOne, aqlTwo)));
    ArgumentCaptor<AqlSpecification> specArgumentCaptor = ArgumentCaptor.forClass(AqlSpecification.class);
    ArgumentCaptor<Pageable> pageableCapture = ArgumentCaptor.forClass(Pageable.class);
    Page<Aql> aqlPage = aqlService.getVisibleAqls("approvedCriteriaEditorId", pageRequest, SearchCriteria.builder()
            .sortBy("category")
            .sort("DESC")
            .build());
    Mockito.verify(aqlRepository, Mockito.times(1)).findAll(specArgumentCaptor.capture(), pageableCapture.capture());
    Pageable capturedInput = pageableCapture.getValue();
    Assert.assertEquals(pageRequest, capturedInput);
    AqlSpecification aqlSpecification = specArgumentCaptor.getValue();
    Assert.assertEquals("approvedCriteriaEditorId", aqlSpecification.getLoggedInUserId());
    Assert.assertEquals("de", aqlSpecification.getLanguage());
    List<Aql> filteredAql = aqlPage.getContent();
    Assert.assertEquals(Long.valueOf(2L), filteredAql.get(0).getId());
  }

  private Aql createAql(OffsetDateTime createdAndModifiedDate) {
    return Aql.builder()
        .id(10L)
        .name("name")
        .use("use")
        .purpose("purpose")
        .nameTranslated("name - en")
        .useTranslated("use - en")
        .purposeTranslated("purpose - en")
        .publicAql(false)
        .createDate(createdAndModifiedDate)
        .modifiedDate(createdAndModifiedDate)
        .build();
  }
}
