package de.vitagroup.num.service;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.CommentRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
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
  private final ProjectService projectService;

  public List<Comment> getComments(Long projectId, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    if (!projectService.exists(projectId)) {
      throw new ResourceNotFound("Project does not exist");
    }

    return commentRepository.findByProjectId(projectId);
  }

  public Comment createComment(Comment comment, Long projectId, String loggedInUserId) {
    UserDetails author = userDetailsService.checkIsUserApproved(loggedInUserId);

    Project project =
        projectService
            .getProjectById(projectId)
            .orElseThrow(() -> new ResourceNotFound("Project not found " + projectId));

    comment.setProject(project);
    comment.setAuthor(author);
    comment.setCreateDate(OffsetDateTime.now());
    return commentRepository.save(comment);
  }

  public Comment updateComment(
      Comment comment, Long commentId, String loggedInUserId, Long projectId) {

    userDetailsService.checkIsUserApproved(loggedInUserId);

    if (!projectService.exists(projectId)) {
      throw new ResourceNotFound("Project not found: " + projectId);
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

  public void deleteComment(Long commentId, Long projectId, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    if (!projectService.exists(projectId)) {
      throw new ResourceNotFound("Project does not exist");
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
