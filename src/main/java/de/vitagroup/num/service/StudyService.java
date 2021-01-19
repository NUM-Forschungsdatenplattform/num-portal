package de.vitagroup.num.service;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StudyService {

  private final StudyRepository studyRepository;
  private final UserDetailsRepository userDetailsRepository;

  public List<Study> getAllStudies() {
    return studyRepository.findAll();
  }

  public Optional<Study> getStudyById(Long studyId) {
    return studyRepository.findById(studyId);
  }

  public boolean exists(Long studyId) {
    return studyRepository.existsById(studyId);
  }

  public Study createStudy(Study study, String userId) {
    Optional<UserDetails> coordinator = userDetailsRepository.findByUserId(userId);

    if (coordinator.isEmpty()) {
      throw new SystemException("Logged in coordinator not found in portal");
    }

    if (coordinator.get().isNotApproved()) {
      throw new ForbiddenException("User not approved:" + userId);
    }

    study.setCoordinator(coordinator.get());
    study.setCreateDate(OffsetDateTime.now());
    study.setModifiedDate(OffsetDateTime.now());

    return studyRepository.save(study);
  }

  public Study updateStudy(Study study, Long id, String loggedInUser) {

    Optional<UserDetails> coordinator = userDetailsRepository.findByUserId(loggedInUser);

    if (coordinator.isEmpty()) {
      throw new SystemException("Logged in coordinator not found in portal");
    }

    if (coordinator.get().isNotApproved()) {
      throw new ForbiddenException("User not approved:" + loggedInUser);
    }

    Study studyToEdit = studyRepository.findById(id).orElseThrow(ResourceNotFound::new);

    studyToEdit.setTemplates(study.getTemplates());
    studyToEdit.setName(study.getName());
    studyToEdit.setDescription(study.getDescription());
    studyToEdit.setResearchers(study.getResearchers());
    studyToEdit.setModifiedDate(OffsetDateTime.now());
    studyToEdit.setStatus(study.getStatus());
    studyToEdit.setFirstHypotheses(study.getFirstHypotheses());
    studyToEdit.setSecondHypotheses(study.getSecondHypotheses());

    return studyRepository.save(studyToEdit);
  }

  public List<Study> searchStudies(String coordinatorUserId) {

    if (StringUtils.isEmpty(coordinatorUserId)) {
      return studyRepository.findAll();
    }

    return studyRepository.findByCoordinatorUserId(coordinatorUserId);
  }
}
