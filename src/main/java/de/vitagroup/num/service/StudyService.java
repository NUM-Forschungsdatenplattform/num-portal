package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.ehrbase.response.openehr.QueryResponseData;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StudyService {

  private final StudyRepository studyRepository;
  private final UserDetailsRepository userDetailsRepository;
  private final UserDetailsService userDetailsService;
  private final EhrBaseService ehrBaseService;
  private final ObjectMapper mapper;

  public String executeAql(String query, Long studyId, String userId) {

    QueryResponseData response = getAqlExecutionResponse(query, studyId, userId);
    try {
      return mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new SystemException("An issue has occurred, cannot execute aql.");
    }
  }

  public QueryResponseData getAqlExecutionResponse(String query, Long studyId, String userId) {

    validateLoggedInUser(userId);

    Study study =
        studyRepository
            .findById(studyId)
            .orElseThrow(() -> new ResourceNotFound("Study not found: " + studyId));

    if (!study.isStudyResearcher(userId)) {
      throw new ForbiddenException("Cannot access this study");
    }

    return ehrBaseService.executeRawQuery(query);
  }

  public void printResponseCsvToStream(
      QueryResponseData queryResponseData, OutputStream outputStream) {
    List<String> paths = new ArrayList<>();

    for (Map<String, String> column : queryResponseData.getColumns()) {
      paths.add(column.get("path"));
    }
    try (CSVPrinter printer =
        CSVFormat.EXCEL
            .withHeader(paths.toArray(new String[] {}))
            .print(new OutputStreamWriter(outputStream))) {

      for (List<Object> row : queryResponseData.getRows()) {
        printer.printRecord(row);
      }
    } catch (IOException e) {
      throw new SystemException("Error while creating the CSV file");
    }
  }

  public Optional<Study> getStudyById(Long studyId) {
    return studyRepository.findById(studyId);
  }

  public boolean exists(Long studyId) {
    return studyRepository.existsById(studyId);
  }

  public Study createStudy(StudyDto studyDto, String userId, List<String> roles) {

    Optional<UserDetails> coordinator = userDetailsRepository.findByUserId(userId);

    if (coordinator.isEmpty()) {
      throw new SystemException("Logged in coordinator not found in portal");
    }

    if (coordinator.get().isNotApproved()) {
      throw new ForbiddenException("User not approved:" + userId);
    }

    Study study = Study.builder().build();

    setTemplates(study, studyDto);
    setResearchers(study, studyDto);

    validateStatus(null, studyDto.getStatus(), roles);
    study.setStatus(studyDto.getStatus());

    study.setName(studyDto.getName());
    study.setDescription(studyDto.getDescription());
    study.setFirstHypotheses(studyDto.getFirstHypotheses());
    study.setSecondHypotheses(studyDto.getSecondHypotheses());
    study.setCoordinator(coordinator.get());
    study.setCreateDate(OffsetDateTime.now());
    study.setModifiedDate(OffsetDateTime.now());
    return studyRepository.save(study);
  }

  public Study updateStudy(StudyDto studyDto, Long id, String userId, List<String> roles) {

    validateLoggedInUser(userId);

    Study studyToEdit =
        studyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound("Study not found: " + id));

    setTemplates(studyToEdit, studyDto);
    setResearchers(studyToEdit, studyDto);

    validateStatus(studyToEdit.getStatus(), studyDto.getStatus(), roles);
    studyToEdit.setStatus(studyDto.getStatus());

    studyToEdit.setName(studyDto.getName());
    studyToEdit.setDescription(studyDto.getDescription());
    studyToEdit.setModifiedDate(OffsetDateTime.now());
    studyToEdit.setFirstHypotheses(studyDto.getFirstHypotheses());
    studyToEdit.setSecondHypotheses(studyDto.getSecondHypotheses());

    return studyRepository.save(studyToEdit);
  }

  public List<Study> getStudies(String userId, List<String> roles) {

    List<Study> studiesList = new ArrayList<>();

    if (roles.contains(Roles.STUDY_COORDINATOR)) {
      studiesList.addAll(studyRepository.findByCoordinatorUserId(userId));
    }
    if (roles.contains(Roles.RESEARCHER)) {
      studiesList.addAll(
          studyRepository.findByResearchers_UserIdAndStatusIn(
              userId, new StudyStatus[] {StudyStatus.PUBLISHED, StudyStatus.CLOSED}));
    }
    if (roles.contains(Roles.STUDY_APPROVER)) {
      studiesList.addAll(
          studyRepository.findByStatusIn(
              new StudyStatus[] {StudyStatus.PENDING, StudyStatus.REVIEWING}));
    }

    return studiesList.stream().distinct().collect(Collectors.toList());
  }

  private void setTemplates(Study study, StudyDto studyDto) {
    if (studyDto.getTemplates() != null) {
      Map<String, String> map =
          studyDto.getTemplates().stream()
              .collect(
                  Collectors.toMap(
                      TemplateInfoDto::getTemplateId, TemplateInfoDto::getName, (t1, t2) -> t1));

      study.setTemplates(map);
    }
  }

  private void setResearchers(Study study, StudyDto studyDto) {
    List<UserDetails> newResearchersList = new LinkedList<>();

    if (studyDto.getResearchers() != null) {
      for (UserDetailsDto dto : studyDto.getResearchers()) {
        Optional<UserDetails> researcher = userDetailsService.getUserDetailsById(dto.getUserId());

        if (researcher.isEmpty()) {
          throw new BadRequestException("Researcher not found.");
        }

        if (researcher.get().isNotApproved()) {
          throw new BadRequestException("Researcher not approved.");
        }

        newResearchersList.add(researcher.get());
      }
    }
    study.setResearchers(newResearchersList);
  }

  private void validateStatus(
      StudyStatus initialStatus, StudyStatus nextStatus, List<String> roles) {

    if (nextStatus == null) {
      throw new BadRequestException("Invalid study status");
    }

    if (initialStatus == null) {
      if (!isValidInitialStatus(nextStatus)) {
        throw new BadRequestException("Invalid study status: " + nextStatus);
      }
    } else if (initialStatus.nextStatusesAndRoles().containsKey(nextStatus)) {
      List<String> allowedRoles = initialStatus.nextStatusesAndRoles().get(nextStatus);

      Set<String> intersectionSet =
          roles.stream().distinct().filter(allowedRoles::contains).collect(Collectors.toSet());

      if (intersectionSet.isEmpty()) {
        throw new ForbiddenException(
            "Study status transition from " + initialStatus + " to " + nextStatus + " not allowed");
      }
    } else {
      throw new BadRequestException(
          "Study status transition from " + initialStatus + " to " + nextStatus + " not allowed");
    }
  }

  private boolean isValidInitialStatus(StudyStatus status) {
    return status.equals(StudyStatus.DRAFT) || status.equals(StudyStatus.PENDING);
  }

  private void validateLoggedInUser(String userId) {
    Optional<UserDetails> user = userDetailsRepository.findByUserId(userId);

    if (user.isEmpty()) {
      throw new SystemException("Logged in coordinator not found in portal");
    }

    if (user.get().isNotApproved()) {
      throw new ForbiddenException("User not approved:" + userId);
    }
  }
}
