package de.vitagroup.num.service;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.CommentRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final UserDetailsService userDetailsService;
  private final StudyService studyService;

  public List<Comment> getComments(Long studyId, String userId) {
    userDetailsService.validateAndReturnUserDetails(userId);
    if (!studyService.exists(studyId)) {
      throw new ResourceNotFound("Study does not exist");
    }

    return commentRepository.findByStudyId(studyId);
  }

  public Comment createComment(Comment comment, Long studyId, String loggedInUserId) {
    UserDetails author = userDetailsService.validateAndReturnUserDetails(loggedInUserId);

    Study study =
        studyService
            .getStudyById(studyId)
            .orElseThrow(() -> new ResourceNotFound("Study not found " + studyId));

    comment.setStudy(study);
    comment.setAuthor(author);
    comment.setCreateDate(OffsetDateTime.now());
    return commentRepository.save(comment);
  }

  public Comment updateComment(
      Comment comment, Long commentId, String loggedInUserId, Long studyId) {

    userDetailsService.validateAndReturnUserDetails(loggedInUserId);

    if (!studyService.exists(studyId)) {
      throw new ResourceNotFound("Study not found: " + studyId);
    }

    Comment commentToEdit =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new ResourceNotFound("Comment not found " + commentId));

    if (commentToEdit.hasEmptyOrDifferentAuthor(loggedInUserId)) {
      throw new ForbiddenException(
          String.format(
              "%s: %s %s.",
              "Comment edit for comment with id",
              commentId,
              "not allowed. Comment has different author"));
    }

    commentToEdit.setText(comment.getText());
    return commentRepository.save(commentToEdit);
  }

  public void deleteComment(Long commentId, Long studyId, String loggedInUserId) {
    userDetailsService.validateAndReturnUserDetails(loggedInUserId);

    if (!studyService.exists(studyId)) {
      throw new ResourceNotFound("Study does not exist");
    }

    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new ResourceNotFound("Comment not found " + commentId));

    if (comment.hasEmptyOrDifferentAuthor(loggedInUserId)) {
      throw new ForbiddenException("Cannot delete comment: " + commentId);
    }

    try {
      commentRepository.deleteById(commentId);
    } catch (EmptyResultDataAccessException e) {
      throw new BadRequestException(String.format("%s: %s", "Invalid commentId id", commentId));
    }
  }
}
