package de.vitagroup.num.service;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COULDN_T_PARSE_CARD;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COULDN_T_PARSE_NAVIGATION_CONTENT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COULDN_T_SAVE_CARD;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COULDN_T_SAVE_NAVIGATION_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.model.Content;
import de.vitagroup.num.domain.model.ContentType;
import de.vitagroup.num.domain.model.Roles;
import de.vitagroup.num.domain.model.admin.UserDetails;
import de.vitagroup.num.domain.dto.CardDto;
import de.vitagroup.num.domain.dto.CardDto.LocalizedPart;
import de.vitagroup.num.domain.dto.MetricsDto;
import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.domain.repository.ContentItemRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.service.exception.SystemException;
import joptsimple.internal.Strings;
import org.ehrbase.response.openehr.QueryResponseData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContentServiceTest {

  @Spy private ObjectMapper mapper;

  @Spy private ContentItemRepository repository;

  @Mock
  private ProjectService projectService;

  @Mock
  private AqlService aqlService;

  @Mock
  private OrganizationService organizationService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private EhrBaseService ehrBaseService;

  @InjectMocks private ContentService contentService;

  private NavigationItemDto navigation1 =
      NavigationItemDto.builder().title("Link 11").url(new URL("https://www.google1.de")).build();
  private NavigationItemDto navigation2 =
      NavigationItemDto.builder().title("Link 22").url(new URL("https://www.google2.de")).build();
  private List<NavigationItemDto> navigations = List.of(navigation1, navigation2);
  private String navigationJson =
      "[{\"title\":\"Link 11\",\"url\":\"https://www.google1.de\"},{\"title\":\"Link 22\",\"url\":\"https://www.google2.de\"}]";

  private CardDto card1 =
      CardDto.builder()
          .en(LocalizedPart.builder().title("Title 1").text("text 1").build())
          .de(LocalizedPart.builder().title("Title de 1").text("text de 1").build())
          .imageId("image1")
          .url(new URL("http://test.test/"))
          .build();
  private CardDto card2 =
      CardDto.builder()
          .en(LocalizedPart.builder().title("Title 2").text("text 2").build())
          .de(LocalizedPart.builder().title("Title de 2").text("text de 2").build())
          .imageId("image2")
          .url(new URL("http://test2.test/"))
          .build();
  private List<CardDto> cards = List.of(card1, card2);
  private String cardJson =
      "[{\"en\":{\"title\":\"Title 1\",\"text\":\"text 1\"},\"de\":{\"title\":\"Title de 1\",\"text\":\"text de 1\"},\"imageId\":\"image1\",\"url\":\"http://test.test/\"},{\"en\":{\"title\":\"Title 2\",\"text\":\"text 2\"},\"de\":{\"title\":\"Title de 2\",\"text\":\"text de 2\"},\"imageId\":\"image2\",\"url\":\"http://test2.test/\"}]";

  public ContentServiceTest() throws MalformedURLException {}

  @Test
  public void shouldCorrectlyRetrieveNoNavigation() {
    reset(repository);
    when(repository.findByType(ContentType.NAVIGATION)).thenReturn(Optional.of(new ArrayList<>()));
    assertEquals("[]", contentService.getNavigationItems());
  }

  @Test
  public void shouldCorrectlyRetrieveNavigation()
      throws MalformedURLException, JsonProcessingException {

    when(repository.findByType(ContentType.NAVIGATION))
        .thenReturn(
            Optional.of(
                List.of(
                    Content.builder()
                        .content(navigationJson)
                        .type(ContentType.NAVIGATION)
                        .id(1L)
                        .build())));

    List<NavigationItemDto> itemDtos =
        mapper.readValue(contentService.getNavigationItems(), new TypeReference<>() {});

    assertEquals(2, itemDtos.size());
    assertEquals("Link 11", itemDtos.get(0).getTitle());
    assertEquals("Link 22", itemDtos.get(1).getTitle());
    assertEquals(new URL("https://www.google1.de"), itemDtos.get(0).getUrl());
    assertEquals(new URL("https://www.google2.de"), itemDtos.get(1).getUrl());
  }

  @Test
  public void shouldCorrectlySaveNavigation() {
    when(repository.save(any())).thenReturn(null);

    contentService.setNavigationItems(navigations);

    ArgumentCaptor<Content> argumentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(repository).save(argumentCaptor.capture());
    Content capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.getType(), is(ContentType.NAVIGATION));
    assertEquals(navigationJson, capturedArgument.getContent());
  }

  @Test
  public void shouldCorrectlyRetrieveNoCards() {
    reset(repository);
    when(repository.findByType(ContentType.CARD)).thenReturn(Optional.of(new ArrayList<>()));
    assertEquals("[]", contentService.getCards());
  }

  @Test
  public void shouldCorrectlyRetrieveCards() throws MalformedURLException, JsonProcessingException {

    when(repository.findByType(ContentType.CARD))
        .thenReturn(
            Optional.of(
                List.of(
                    Content.builder().content(cardJson).type(ContentType.CARD).id(1L).build())));

    String json = contentService.getCards();
    List<CardDto> itemDtos = mapper.readValue(json, new TypeReference<>() {});

    assertEquals(2, itemDtos.size());
    assertEquals("Title 1", itemDtos.get(0).getEn().getTitle());
    assertEquals("Title 2", itemDtos.get(1).getEn().getTitle());
    assertEquals(new URL("http://test.test/"), itemDtos.get(0).getUrl());
    assertEquals(new URL("http://test2.test/"), itemDtos.get(1).getUrl());
    reset(repository);
  }

  @Test
  public void shouldCorrectlySaveCards() {
    when(repository.save(any())).thenReturn(null);

    contentService.setCards(cards);

    ArgumentCaptor<Content> argumentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(repository).save(argumentCaptor.capture());
    Content capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.getType(), is(ContentType.CARD));
    assertEquals(cardJson, capturedArgument.getContent());
  }

  @Test(expected = SystemException.class)
  public void getNavigationItemsParsingErrors() {
    when(repository.findByType(ContentType.NAVIGATION))
            .thenThrow(new SystemException(ContentService.class, COULDN_T_PARSE_NAVIGATION_CONTENT,
                    String.format(COULDN_T_PARSE_NAVIGATION_CONTENT, Strings.EMPTY)));
    contentService.getNavigationItems();
  }

  @Test(expected = SystemException.class)
  public void setNavigationItemsParsingErrors() {
    when(repository.findByType(ContentType.NAVIGATION))
            .thenThrow(new SystemException(ContentService.class, COULDN_T_SAVE_NAVIGATION_CONTENT, String.format(COULDN_T_SAVE_NAVIGATION_CONTENT, Strings.EMPTY)));
    contentService.setNavigationItems(Collections.singletonList(new NavigationItemDto()));
  }

  @Test(expected = SystemException.class)
  public void getCardsItemsParsingErrors() {
    when(repository.findByType(ContentType.CARD))
            .thenThrow(new SystemException(ContentService.class, COULDN_T_PARSE_CARD,
                    String.format(COULDN_T_PARSE_CARD, Strings.EMPTY)));
    contentService.getCards();
  }

  @Test(expected = SystemException.class)
  public void setCardsItemsParsingErrors() {
    when(repository.findByType(ContentType.CARD))
            .thenThrow(new SystemException(ContentService.class, COULDN_T_SAVE_CARD,
                    String.format(COULDN_T_SAVE_CARD, Strings.EMPTY)));
    contentService.setCards(new ArrayList<>());
  }

  @Test
  public void shouldGetLatestProjects() {
    contentService.getLatestProjects(List.of(Roles.STUDY_COORDINATOR));
    verify(projectService, times(1)).getLatestProjectsInfo(5, List.of(Roles.STUDY_COORDINATOR));
  }

  @Test
  public void shouldGetMetrics() {
    Mockito.when(aqlService.countAqls()).thenReturn(50L);
    Mockito.when(projectService.countProjects()).thenReturn(100L);
    Mockito.when(organizationService.countOrganizations()).thenReturn(12L);
    MetricsDto respone = contentService.getMetrics();
    assertEquals(50L, respone.getAqls());
    assertEquals(100L, respone.getProjects());
    assertEquals(12, respone.getOrganizations());
  }

  @Test
  public void shouldGetClinics() {
    mockGetClinicsData();
    List<String> clinics = contentService.getClinics("approvedUserId");
    assertEquals(2, clinics.size());
  }

  @Test
  public void shouldGetClinicDistributions() {
    QueryResponseData responseData = new QueryResponseData();
    responseData.setColumns(
            new ArrayList<>(List.of(Map.of("path", "/count(r/data[at0001]/events[at0002]/data[at0003]/items[at0041]/value/magnitude)"),
                    Map.of("name", "sofa_score"))));
    responseData.setRows(List.of(new ArrayList<>(List.of(25))));
    Mockito.when(ehrBaseService.executePlainQuery(Mockito.contains("openEHR-EHR-OBSERVATION.sofa_score.v0"))).thenReturn(responseData);
    Map<String, Integer> distribution = contentService.getClinicDistributions("dummy clinic");
    assertTrue(distribution.containsKey("0-4"));
    assertEquals(25, distribution.get("0-4"));
  }

  @Test
  public void shouldGetClinicAverages() {
    QueryResponseData responseData = new QueryResponseData();
    responseData.setColumns(
            new ArrayList<>(List.of(Map.of("path", "/avg(r/data[at0001]/events[at0002]/data[at0003]/items[at0041]/value/magnitude)"),
                    Map.of("name", "sofa_avg"))));
    responseData.setRows(List.of(new ArrayList<>(List.of(12.33))));
    Mockito.when(ehrBaseService.executePlainQuery(Mockito.contains("avg(r/data[at0001]/events[at0002]/data[at0003]/items[at0041]/value/magnitude) as sofa_avg"))).thenReturn(responseData);
    mockGetClinicsData();
    Map<String, Double> clinicAverages = contentService.getClinicAverages("approvedUserId");
    assertTrue(clinicAverages.containsKey("Hospital"));
    assertEquals(12.33, clinicAverages.get("Hospital"));
  }

  private void mockGetClinicsData () {
    Mockito.when(userDetailsService.checkIsUserApproved("approvedUserId"))
            .thenReturn(UserDetails.builder()
                    .userId("approvedUserId")
                    .approved(true)
                    .build());
    QueryResponseData responseData = new QueryResponseData();
    responseData.setColumns(
            new ArrayList<>(List.of(Map.of("path", "/context/health_care_facility/name"), Map.of("name", "health_care_facility"))));
    responseData.setRows(List.of(new ArrayList<>(List.of("Hospital")),
            new ArrayList<>(List.of("Medizinische Hochschule Hannover"))));
    Mockito.when(ehrBaseService.executePlainQuery(Mockito.eq(ContentService.LIST_CLINICS))).thenReturn(responseData);
  }
}
