package de.vitagroup.num.service;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.repository.StudyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class StudyService {

  private final StudyRepository studyRepository;

  public List<Study> getAllStudies() {
    return studyRepository.findAll();
  }

  public Optional<Study> getStudyById(Long studyId) {
    return studyRepository.findById(studyId);
  }

  public Study createStudy(Study study) {
    return studyRepository.save(study);
  }
}
