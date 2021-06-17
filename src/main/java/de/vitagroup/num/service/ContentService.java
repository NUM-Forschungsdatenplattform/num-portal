package de.vitagroup.num.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Content;
import de.vitagroup.num.domain.ContentType;
import de.vitagroup.num.domain.dto.CardDto;
import de.vitagroup.num.domain.dto.MetricsDto;
import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.domain.dto.ProjectInfoDto;
import de.vitagroup.num.domain.repository.ContentItemRepository;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.response.openehr.QueryResponseData;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ContentService {

  private final ContentItemRepository contentItemRepository;

  private final ObjectMapper mapper;

  private final ProjectService projectService;

  private final AqlService aqlService;

  private final OrganizationService organizationService;

  private final EhrBaseService ehrBaseService;

  private static final int PROJECT_COUNT = 5;

  private static final int SOFA_MIN = 0;
  private static final int SOFA_MAX = 24;
  private static final int SOFA_INTERVAL_LEN = 5;

  private static final String LIST_CLINICS =
      "SELECT distinct c/context/health_care_facility/name as health_care_facility FROM EHR e CONTAINS COMPOSITION c";

  private static final String GET_CLINIC_SOFA_AVG =
      "SELECT avg(r/data[at0001]/events[at0002]/data[at0003]/items[at0041]/value/magnitude) as sofa_avg "
          + "FROM EHR e CONTAINS COMPOSITION c CONTAINS OBSERVATION r[openEHR-EHR-OBSERVATION.sofa_score.v0] "
          + "WHERE c/context/health_care_facility/name = '%s'";

  private static final String GET_CLINIC_SOFA_COUNT_IN_INTERVAL =
      "SELECT count(r/data[at0001]/events[at0002]/data[at0003]/items[at0041]/value/magnitude) as sofa_score "
          + "FROM EHR e CONTAINS COMPOSITION c CONTAINS OBSERVATION r[openEHR-EHR-OBSERVATION.sofa_score.v0] "
          + "WHERE r/data[at0001]/events[at0002]/data[at0003]/items[at0041]/value/magnitude >= %d "
          + "AND r/data[at0001]/events[at0002]/data[at0003]/items[at0041]/value/magnitude <= %d "
          + "AND c/context/health_care_facility/name = '%s'";

  /**
   * Retrieves info about the latest five projects
   *
   * @return the five latest projects
   */
  public List<ProjectInfoDto> getLatestProjects() {
    return projectService.getLatestProjectsInfo(PROJECT_COUNT);
  }

  /**
   * Computes platform metrics, number of projects, number of organizations and number of aqls
   *
   * @return the metrics
   */
  public MetricsDto getMetrics() {
    return MetricsDto.builder()
        .aqls(aqlService.countAqls())
        .projects(projectService.countProjects())
        .organizations(organizationService.countOrganizations())
        .build();
  }

  public String getNavigationItems() {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.NAVIGATION).orElse(new ArrayList<>());
    if (contents.isEmpty()) {
      return "[]";
    } else {
      try {
        return contents.get(0).getContent();
      } catch (Exception e) {
        log.error("Couldn't parse navigation content", e);
        throw new SystemException("Couldn't parse navigation content", e);
      }
    }
  }

  public void setNavigationItems(List<NavigationItemDto> navigationItemDtos) {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.NAVIGATION).orElse(new ArrayList<>());
    Content navigation;
    if (contents.isEmpty()) {
      navigation = Content.builder().type(ContentType.NAVIGATION).build();
    } else {
      navigation = contents.get(0);
    }

    try {
      navigation.setContent(mapper.writeValueAsString(navigationItemDtos));
      contentItemRepository.save(navigation);
    } catch (Exception e) {
      log.error("Couldn't save navigation content", e);
      throw new SystemException("Couldn't save navigation content", e);
    }
  }

  public String getCards() {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.CARD).orElse(new ArrayList<>());
    if (contents.isEmpty()) {
      return "[]";
    } else {
      try {
        return contents.get(0).getContent();
      } catch (Exception e) {
        log.error("Couldn't parse card", e);
        throw new SystemException("Couldn't parse card", e);
      }
    }
  }

  public void setCards(List<CardDto> cardDtos) {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.CARD).orElse(new ArrayList<>());
    Content navigation;
    if (contents.isEmpty()) {
      navigation = Content.builder().type(ContentType.CARD).build();
    } else {
      navigation = contents.get(0);
    }

    try {
      navigation.setContent(mapper.writeValueAsString(cardDtos));
      contentItemRepository.save(navigation);
    } catch (Exception e) {
      log.error("Couldn't save card", e);
      throw new SystemException("Couldn't save card", e);
    }
  }

  public List<String> getClinics() {
    QueryResponseData responseData = ehrBaseService.executePlainQuery(LIST_CLINICS);
    return responseData.getRows().stream()
        .map(row -> (String) row.get(0))
        .collect(Collectors.toList());
  }

  public Map<String, Integer> getClinicDistributions(String name) {
    Map<String, Integer> distributions = new LinkedHashMap<>();
    for (int i = SOFA_MIN; i < SOFA_MAX; i += SOFA_INTERVAL_LEN) {
      int end = i + SOFA_INTERVAL_LEN - 1;
      String interval = i + "-" + end;
      QueryResponseData queryResponseData =
          ehrBaseService.executePlainQuery(
              String.format(GET_CLINIC_SOFA_COUNT_IN_INTERVAL, i, end, name));
      List<List<Object>> rows = queryResponseData.getRows();
      if (rows.size() == 1 && rows.get(0).size() == 1 && rows.get(0).get(0) != null) {
        distributions.put(interval, (Integer) rows.get(0).get(0));
      } else {
        distributions.put(interval, 0);
      }
    }
    return distributions;
  }

  public Map<String, Double> getClinicAverages() {
    Map<String, Double> averages = new LinkedHashMap<>();
    List<String> clinics = getClinics();
    for (String clinic : clinics) {
      QueryResponseData queryResponseData =
          ehrBaseService.executePlainQuery(String.format(GET_CLINIC_SOFA_AVG, clinic));
      List<List<Object>> rows = queryResponseData.getRows();
      if (rows.size() == 1 && rows.get(0).size() == 1 && rows.get(0).get(0) != null) {
        averages.put(clinic, (Double) rows.get(0).get(0));
      } else {
        averages.put(clinic, 0.0);
      }
    }
    return averages;
  }
}
