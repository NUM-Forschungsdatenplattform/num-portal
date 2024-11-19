package org.highmed.numportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ehrbase.aqleditor.dto.aql.QueryValidationResponse;
import org.ehrbase.aqleditor.dto.aql.Result;
import org.ehrbase.aqleditor.service.AqlEditorAqlService;
import org.highmed.numportal.service.exception.*;
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
import org.highmed.numportal.domain.dto.Language;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.dto.SlimAqlDto;
import org.highmed.numportal.domain.model.Aql;
import org.highmed.numportal.domain.model.AqlCategory;
import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.AqlCategoryRepository;
import org.highmed.numportal.domain.repository.AqlRepository;
import org.highmed.numportal.domain.specification.AqlSpecification;
import org.highmed.numportal.properties.PrivacyProperties;
import org.highmed.numportal.service.ehrbase.EhrBaseService;

import java.time.OffsetDateTime;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.*;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.USER_NOT_FOUND;

@RunWith(MockitoJUnitRunner.class)
public class AqlServiceTest {

  @Mock private AqlRepository aqlRepository;

  @Mock private AqlCategoryRepository aqlCategoryRepository;

  @Mock private UserDetailsService userDetailsService;

  @Mock
  private UserService userService;
  @Mock
  private AqlEditorAqlService aqlEditorAqlService;
  @Mock
  private EhrBaseService ehrBaseService;

  @Mock
  private PrivacyProperties privacyProperties;

  @Mock
  private ObjectMapper mapper;

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

    Map<String, String> translations = new HashMap<>();
    translations.put("en", "aql category name en");
    translations.put("de", "aql category name test de");
    when(aqlCategoryRepository.findById(3L)).thenReturn(Optional.of(AqlCategory.builder()
            .id(3L)
            .name(translations)
            .build()));

    Map<String, String> tr1 = new HashMap<>();
    tr1.put("en", "category one in english");
    tr1.put("de", "category one in german");
    Aql aqlOne = Aql.builder()
            .id(1L)
            .name("aql test one")
            .publicAql(true)
            .query("select * from dummy_table")
            .category(AqlCategory.builder().id(1L).name(tr1).build())
            .owner(UserDetails.builder()
                    .userId("approvedUserId")
                    .organization(Organization.builder().name("organization aa").id(1L).build())
                    .build())
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
            .owner(UserDetails.builder()
                    .userId("2ndApprovedUserId")
                    .organization(Organization.builder().name("organization bb").id(2L).build())
                    .build())
            .build();
    Mockito.when(aqlRepository.findAll(Mockito.any(AqlSpecification.class), Mockito.any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(aqlOne, aqlTwo)));
    when(userService.getOwner("approvedUserId")).thenReturn(User.builder().id("approvedUserId").firstName("Approver first name").lastName("Doe").build());
    when(userService.getOwner("2ndApprovedUserId")).thenReturn(User.builder().id("2ndApprovedUserId").firstName("Second approver first name").lastName("Doe").build());
    when(privacyProperties.getMinHits()).thenReturn(2);
    Mockito.when(aqlEditorAqlService.validateAql(Mockito.any(Result.class)))
            .thenReturn(QueryValidationResponse.builder()
                                               .valid(true)
                                               .build()
            );
  }

  @Test
  public void getAqlByIdTest() {
    aqlService.getAqlById(1L, "approvedUserId");
    Mockito.verify(aqlRepository, Mockito.times(1)).findById(Mockito.eq(1L));
  }
  @Test
  public void getAqlSizeTest() {
    SlimAqlDto aqlDto = SlimAqlDto.builder()
            .query("select * from dummy_table")
            .build();
    Mockito.when(ehrBaseService.retrieveNumberOfPatients(Mockito.any(Aql.class))).thenReturn(4);
    aqlService.getAqlSize(aqlDto, "4");
  }

  @Test(expected = PrivacyException.class)
  public void shouldHandlePrivacyExceptionWhenGetAqlSize() {
    SlimAqlDto aqlDto = SlimAqlDto.builder()
            .query("select * from dummy_table")
            .build();
    Mockito.when(ehrBaseService.retrieveNumberOfPatients(Mockito.any(Aql.class))).thenReturn(1);
    aqlService.getAqlSize(aqlDto, "4");
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleInvalidQueryWhenGetAqlSize() {
    SlimAqlDto aqlDto = SlimAqlDto.builder()
            .query("select * from another_table")
            .build();
    Result queryResult = Result.builder().q(aqlDto.getQuery()).build();
    Mockito.when(aqlEditorAqlService.validateAql(Mockito.eq(queryResult)))
            .thenReturn(QueryValidationResponse.builder()
                    .valid(false)
                    .build()
            );
    aqlService.getAqlSize(aqlDto, "4");
  }

  @Test
  public void shouldSuccessfullyCreateAqlWithCategory() {
    Aql toSave = createAql(OffsetDateTime.now());
    Aql createdAql = aqlService.createAql(toSave, "approvedUserId", 3L);
    createAqlChecks(toSave, createdAql);
  }

  @Test
  public void shouldSuccessfullyCreateAql() {
    Aql toSave = createAql(OffsetDateTime.now());
    Aql createdAql = aqlService.createAql(toSave, "approvedUserId", null);
    createAqlChecks(toSave, createdAql);
    Mockito.verifyNoInteractions(aqlCategoryRepository);
  }

  private void createAqlChecks(Aql toSave, Aql createdAql) {
    assertThat(createdAql, notNullValue());
    assertThat(createdAql.getName(), is(toSave.getName()));
    assertThat(createdAql.getUse(), is(toSave.getUse()));
    assertThat(createdAql.getPurpose(), is(toSave.getPurpose()));
    assertThat(createdAql.getNameTranslated(), is(toSave.getNameTranslated()));
    assertThat(createdAql.getUseTranslated(), is(toSave.getUseTranslated()));
    assertThat(createdAql.getPurposeTranslated(), is(toSave.getPurposeTranslated()));
    assertThat(createdAql.isPublicAql(), is(toSave.isPublicAql()));
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleMissingAqlCategory() {
    aqlService.createAql(Aql.builder().build(), "approvedUserId", 99L);
  }

  @Test
  public void shouldSuccessfullyEditAql() {
    Aql toEdit = createAql(OffsetDateTime.now());
    Aql updatedAql = aqlService.updateAql(toEdit, 1L, "approvedUserId", null);

    assertThat(updatedAql, notNullValue());
    assertThat(updatedAql.getName(), is(toEdit.getName()));
    assertThat(updatedAql.getUse(), is(toEdit.getUse()));
    assertThat(updatedAql.getPurpose(), is(toEdit.getPurpose()));
    assertThat(updatedAql.isPublicAql(), is(toEdit.isPublicAql()));
    Mockito.verifyNoInteractions(aqlCategoryRepository);
  }

  @Test
  public void shouldSuccessfullyEditAqlWithCategory() {
    Aql toEdit = createAql(OffsetDateTime.now());
    Aql updatedAql = aqlService.updateAql(toEdit, 1L, "approvedUserId", 3L);

    assertThat(updatedAql, notNullValue());
    assertThat(updatedAql.getName(), is(toEdit.getName()));
    assertThat(updatedAql.getUse(), is(toEdit.getUse()));
    assertThat(updatedAql.getPurpose(), is(toEdit.getPurpose()));
    assertThat(updatedAql.isPublicAql(), is(toEdit.isPublicAql()));
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleMissingAqlOwnerWhenEditing() {
    Aql toEdit = createAql(OffsetDateTime.now());
    aqlService.updateAql(toEdit, 2L, "approvedUserId", null);
  }

  @Test(expected = ForbiddenException.class)
  public void shouldFailWhenDeletingIfManager() {
    aqlService.deleteById(1L, "approvedUserId", List.of(Roles.MANAGER));
    verify(aqlRepository, Mockito.never()).deleteById(1L);
  }

  @Test(expected = SystemException.class)
  public void shouldHandleMissingOwner() {
    aqlService.createAql(Aql.builder().build(), "nonExistingUser", null);
  }

  @Test(expected = ForbiddenException.class)
  public void shouldHandleNotApprovedOwner() {
    aqlService.createAql(Aql.builder().build(), "notApprovedId", null);
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
    aqlService.updateAql(new Aql(), 1000L, "approvedUserId", null);
  }

  @Test(expected = ForbiddenException.class)
  public void updateAqlForbiddenException() {
    aqlService.updateAql(new Aql(), 2L, "approvedUserId", null);
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
    aqlService.updateAqlCategory("approvedUserId", new AqlCategory(),null);
  }

  @Test(expected = ResourceNotFound.class)
  public void updateAqlCategoryResourceNotFound() {
    aqlService.updateAqlCategory("approvedUserId", new AqlCategory(),1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteCategoryById() {
    aqlService.deleteCategoryById("approvedUserId", null);
  }

  @Test(expected = BadRequestException.class)
  public void deleteCategoryByIdBadRequestException() {
    when(aqlRepository.findByCategoryId(null)).thenReturn(List.of(new Aql()));
    aqlService.deleteCategoryById("approvedUserId",null);
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
    aqlService.deleteCategoryById("approvedUserId", 1L);
    verify(aqlCategoryRepository, times(0)).deleteById(1L);
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleCategoryInUseWhenDeleting() {
    when(aqlRepository.findByCategoryId(1L)).thenReturn(List.of(new Aql()));
    aqlService.deleteCategoryById("approvedUserId",1L);
    verify(aqlCategoryRepository, times(0)).deleteById(1L);
  }

  @Test
  public void shouldDeleteCategory() {
    when(aqlRepository.findByCategoryId(1L)).thenReturn(List.of());
    when(aqlCategoryRepository.existsById(1L)).thenReturn(true);
    aqlService.deleteCategoryById("approvedUserId", 1L);
    verify(aqlCategoryRepository, times(1)).deleteById(1L);
  }

  @Test
  public void shouldCreateValidCategory() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).build();
    aqlService.createAqlCategory("approvedUserId",category);
    verify(aqlCategoryRepository, times(1)).save(category);
  }

  @Test
  public void shouldUpdateValidCategory() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).id(1L).build();
    when(aqlCategoryRepository.existsById(1L)).thenReturn(true);
    aqlService.updateAqlCategory("approvedUserId", category, 1L);
    verify(aqlCategoryRepository, times(1)).save(category);
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldNotUpdateNotExistingCategory() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).id(1L).build();
    when(aqlCategoryRepository.existsById(1L)).thenReturn(false);
    aqlService.updateAqlCategory("approvedUserId", category, 1L);
    verify(aqlCategoryRepository, times(0)).save(category);
  }

  @Test(expected = BadRequestException.class)
  public void shouldNotUpdateCategoryWithoutId() {
    Map<String, String> translations = new HashMap<>();
    translations.put("en", "test en");
    translations.put("de", "test de");
    AqlCategory category = AqlCategory.builder().name(translations).id(1L).build();
    aqlService.updateAqlCategory("approvedUserId", category, null);
    verify(aqlCategoryRepository, times(0)).save(category);
  }

  @Test
  public void shouldGetAllCategories() {
    aqlService.getAqlCategories();
    verify(aqlCategoryRepository, times(1)).findAllCategories();
  }

  @Test
  public void getAqlCategoriesWithPaginationTest() {
    Pageable pageable = PageRequest.of(0,30).withSort(JpaSort.unsafe(Sort.Direction.ASC, "name->>'de'"));
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

  @Test(expected = BadRequestException.class)
  public void shouldHandleInvalidSortFieldWhenGetAqlCategories() {
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sortBy("DESC")
            .sort("invalid field")
            .build();
    aqlService.getAqlCategories(PageRequest.of(0,50), searchCriteria);
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
    Assert.assertEquals(Language.de, aqlSpecification.getLanguage());
  }

  @Test
  public void getVisibleAqls() {
    Pageable pageRequest = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createDate"));
    ArgumentCaptor<AqlSpecification> specArgumentCaptor = ArgumentCaptor.forClass(AqlSpecification.class);
    ArgumentCaptor<Pageable> pageableCapture = ArgumentCaptor.forClass(Pageable.class);
    aqlService.getVisibleAqls("approvedCriteriaEditorId", pageRequest, SearchCriteria.builder().build());
    Mockito.verify(aqlRepository, Mockito.times(1)).findAll(specArgumentCaptor.capture(), pageableCapture.capture());
    Pageable capturedInput = pageableCapture.getValue();
    Assert.assertEquals(pageRequest, capturedInput);
    AqlSpecification aqlSpecification = specArgumentCaptor.getValue();
    Assert.assertEquals("approvedCriteriaEditorId", aqlSpecification.getLoggedInUserId());
    Assert.assertEquals(Language.de, aqlSpecification.getLanguage());
    Assert.assertNull(aqlSpecification.getFilter());
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
  public void getVisibleAqlsWithPaginationAndSortByCategoryNameAsc() {
    List<Aql> filteredAql = getAqlsSortByCategoryName("ASC");
    Assert.assertEquals(Long.valueOf(1L), filteredAql.get(0).getId());
  }

  private List<Aql> getAqlsSortByCategoryName(String sortDir) {
    Pageable pageRequest = PageRequest.of(0, 100).withSort(Sort.by(Sort.Direction.valueOf(sortDir), "category"));
    ArgumentCaptor<AqlSpecification> specArgumentCaptor = ArgumentCaptor.forClass(AqlSpecification.class);
    ArgumentCaptor<Pageable> pageableCapture = ArgumentCaptor.forClass(Pageable.class);
    Page<Aql> aqlPage = aqlService.getVisibleAqls("approvedCriteriaEditorId", pageRequest, SearchCriteria.builder()
            .sortBy("category")
            .sort(sortDir)
            .build());
    Mockito.verify(aqlRepository, Mockito.times(1)).findAll(specArgumentCaptor.capture(), pageableCapture.capture());
    Pageable capturedInput = pageableCapture.getValue();
    Assert.assertEquals(PageRequest.of(0, 100), capturedInput);
    AqlSpecification aqlSpecification = specArgumentCaptor.getValue();
    Assert.assertEquals("approvedCriteriaEditorId", aqlSpecification.getLoggedInUserId());
    Assert.assertEquals(Language.de, aqlSpecification.getLanguage());
    Assert.assertEquals(Sort.Order.asc("category"), aqlSpecification.getSortOrder());
    return aqlPage.getContent();
  }

  @Test
  public void getVisibleAqlsWithPaginationAndSortByAuthorAsc() {
    List<Aql> filteredAql = getVisibleAqlsWithSortByAuthor("ASC");
    Assert.assertEquals(Long.valueOf(1L), filteredAql.get(0).getId());
  }
  @Test
  public void getVisibleAqlsWithPaginationAndSortByAuthorDesc() {
    List<Aql> filteredAql = getVisibleAqlsWithSortByAuthor("DESC");
    Assert.assertEquals(Long.valueOf(2L), filteredAql.get(0).getId());
  }

  private List<Aql> getVisibleAqlsWithSortByAuthor(String sortDir) {
    Pageable pageRequest = PageRequest.of(0, 100);
    ArgumentCaptor<AqlSpecification> specArgumentCaptor = ArgumentCaptor.forClass(AqlSpecification.class);
    ArgumentCaptor<Pageable> pageableCapture = ArgumentCaptor.forClass(Pageable.class);
    Map<String, String> filter = new HashMap<>();
    filter.put(SearchCriteria.FILTER_SEARCH_BY_KEY, "search dummy");
    Mockito.when(aqlRepository.count()).thenReturn(100L);
    Page<Aql> aqlPage = aqlService.getVisibleAqls("approvedCriteriaEditorId", pageRequest, SearchCriteria.builder()
            .sortBy("author")
            .sort(sortDir)
            .filter(filter)
            .build());
    Mockito.verify(aqlRepository, Mockito.times(1)).findAll(specArgumentCaptor.capture(), pageableCapture.capture());
    Pageable capturedInput = pageableCapture.getValue();
    Assert.assertEquals(pageRequest, capturedInput);
    AqlSpecification aqlSpecification = specArgumentCaptor.getValue();
    Assert.assertEquals("approvedCriteriaEditorId", aqlSpecification.getLoggedInUserId());
    return aqlPage.getContent();
  }

  @Test
  public void getVisibleAqlsWithPaginationAndSortByOrganizationAsc() {
    List<Aql> filteredAql = getVisibleAqlsWithPaginationAndSortByOrganizationName("ASC");
    Assert.assertEquals(Long.valueOf(1L), filteredAql.get(0).getId());
  }

  private List<Aql> getVisibleAqlsWithPaginationAndSortByOrganizationName(String sortDir) {
    Pageable pageRequest = PageRequest.of(0, 100, Sort.by(Sort.Direction.valueOf(sortDir), "owner.organization.name"));
    ArgumentCaptor<AqlSpecification> specArgumentCaptor = ArgumentCaptor.forClass(AqlSpecification.class);
    ArgumentCaptor<Pageable> pageableCapture = ArgumentCaptor.forClass(Pageable.class);
    Page<Aql> aqlPage = aqlService.getVisibleAqls("approvedCriteriaEditorId", pageRequest, SearchCriteria.builder()
            .sortBy("organization")
            .sort(sortDir)
            .build());
    Mockito.verify(aqlRepository, Mockito.times(1)).findAll(specArgumentCaptor.capture(), pageableCapture.capture());
    Pageable capturedInput = pageableCapture.getValue();
    Assert.assertEquals(pageRequest, capturedInput);
    AqlSpecification aqlSpecification = specArgumentCaptor.getValue();
    Assert.assertEquals("approvedCriteriaEditorId", aqlSpecification.getLoggedInUserId());
    return aqlPage.getContent();
  }

  @Test(expected = BadRequestException.class)
  public void shouldHandleMissingSortField() {
    SearchCriteria searchCriteria = SearchCriteria.builder()
            .sortBy("ASC")
            .build();
    aqlService.getVisibleAqls("approvedUserId", PageRequest.of(0,50), searchCriteria);
    verify(aqlRepository, never());
  }

  @Test
  public void aqlCategoryIsAllowedToBeDeletedTest() {
    boolean result = aqlService.aqlCategoryIsAllowedToBeDeleted(3L);
    Assert.assertTrue(result);
  }

  @Test(expected = ResourceNotFound.class)
  public void shouldHandleNotExistingCategoryWhenIsAllowedToBeDeleted() {
    aqlService.aqlCategoryIsAllowedToBeDeleted(33L);
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
