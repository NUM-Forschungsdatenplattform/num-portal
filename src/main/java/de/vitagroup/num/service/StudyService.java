package de.vitagroup.num.service;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StudyService {

  private final StudyRepository studyRepository;
  private final UserDetailsRepository userDetailsRepository;
  private final UserDetailsService userDetailsService;
  private final ModelMapper modelMapper;

  public List<Study> getAllStudies() {
    return studyRepository.findAll();
  }

  public Optional<Study> getStudyById(Long studyId) {
    return studyRepository.findById(studyId);
  }

  public boolean exists(Long studyId) {
    return studyRepository.existsById(studyId);
  }

  public Study createStudy(StudyDto studyDto, String userId) {
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

    study.setName(studyDto.getName());
    study.setDescription(studyDto.getDescription());
    study.setFirstHypotheses(studyDto.getFirstHypotheses());
    study.setSecondHypotheses(studyDto.getSecondHypotheses());
    study.setStatus(studyDto.getStatus());
    study.setCoordinator(coordinator.get());
    study.setCreateDate(OffsetDateTime.now());
    study.setModifiedDate(OffsetDateTime.now());
    return studyRepository.save(study);
  }

  public Study updateStudy(StudyDto studyDto, Long id, String loggedInUser) {

    Optional<UserDetails> coordinator = userDetailsRepository.findByUserId(loggedInUser);

    if (coordinator.isEmpty()) {
      throw new SystemException("Logged in coordinator not found in portal");
    }

    if (coordinator.get().isNotApproved()) {
      throw new ForbiddenException("User not approved:" + loggedInUser);
    }

    Study studyToEdit = studyRepository.findById(id).orElseThrow(ResourceNotFound::new);

    setTemplates(studyToEdit, studyDto);
    setResearchers(studyToEdit, studyDto);

    studyToEdit.setName(studyDto.getName());
    studyToEdit.setDescription(studyDto.getDescription());
    studyToEdit.setModifiedDate(OffsetDateTime.now());
    studyToEdit.setStatus(studyDto.getStatus());
    studyToEdit.setFirstHypotheses(studyDto.getFirstHypotheses());
    studyToEdit.setSecondHypotheses(studyDto.getSecondHypotheses());

    return studyRepository.save(studyToEdit);
  }

  public List<Study> searchStudies(String userId, List<String> roles) {

    List<Study> studiesList = new ArrayList<>();

    if (roles.contains(Roles.STUDY_COORDINATOR)) {
      studiesList.addAll(studyRepository.findByCoordinatorUserId(userId));
    }
    if (roles.contains(Roles.RESEARCHER)) {
      studiesList.addAll(
          studyRepository.findByResearchers_UserIdAndStatus(userId, StudyStatus.PUBLISHED));
      studiesList.addAll(
          studyRepository.findByResearchers_UserIdAndStatus(userId, StudyStatus.CLOSED));
    }
    if (roles.contains(Roles.STUDY_APPROVER)) {
      studiesList.addAll(studyRepository.findByStatus(StudyStatus.PENDING));
      studiesList.addAll(studyRepository.findByStatus(StudyStatus.REVIEWING));
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
}
