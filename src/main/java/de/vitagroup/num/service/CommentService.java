package de.vitagroup.num.service;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.CommentRepository;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final UserDetailsService userDetailsService;
  private final StudyService studyService;

  public List<Comment> getComments(Long studyId) {
    studyService.getStudyById(studyId).orElseThrow(ResourceNotFound::new);
    return commentRepository.findByStudyId(studyId);
  }

  public Comment createComment(Comment comment, Long studyId, String loggedInUserId) {
    UserDetails author =
        userDetailsService.getUserDetailsById(loggedInUserId).orElseThrow(SystemException::new);

    if (author.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in user is not approved.");
    }

    Study study = studyService.getStudyById(studyId).orElseThrow(ResourceNotFound::new);

    comment.setStudy(study);
    comment.setAuthor(author);
    comment.setCreateDate(OffsetDateTime.now());
    return commentRepository.save(comment);
  }
}
