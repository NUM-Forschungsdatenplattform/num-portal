package de.vitagroup.num.service;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.StudyRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.NotAuthorizedException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class StudyService {

  private final StudyRepository studyRepository;
  private final UserDetailsRepository userDetailsRepository;

  public List<Study> getAllStudies() {
    return studyRepository.findAll();
  }

  public List<Study> getAllStudies() {
    return studyRepository.findAll();
  }

  public Optional<Study> getStudyById(Long studyId) {
    return studyRepository.findById(studyId);
  }
  
  public Study createStudy(Study study, String userId) {
    Optional<UserDetails> coordinator = userDetailsRepository.findByUserId(userId);
    // TODO: check role of the logged in coordinator -> need to defined available roles

    if (coordinator.isEmpty()) {
      throw new ResourceNotFound("Logged in coordinator not found in portal");
    }

    if (coordinator.get().isNotApproved()) {
      throw new NotAuthorizedException("User not approved:" + userId);
    }

    study.setCoordinator(coordinator.get());

    return studyRepository.save(study);
  }

  public Study updateStudy(Study study, Long id) {
    Optional<Study> studyToEdit = studyRepository.findById(id);

    if (studyToEdit.isEmpty()) {
      throw new ResourceNotFound("Study not found: " + id);
    }

    studyToEdit.get().setTemplates(study.getTemplates());
    studyToEdit.get().setName(study.getName());
    studyToEdit.get().setDescription(study.getDescription());
    studyToEdit.get().setResearchers(study.getResearchers());

    return studyRepository.save(studyToEdit.get());
  }
}
