package org.highmed.numportal.service;

import org.highmed.numportal.domain.model.Comment;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.CommentRepository;
import org.highmed.numportal.service.exception.BadRequestException;
import org.highmed.numportal.service.exception.ForbiddenException;
import org.highmed.numportal.service.exception.ResourceNotFound;

import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_DELETE_COMMENT;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.COMMENT_NOT_FOUND;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.INVALID_COMMENT_ID;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.PROJECT_DOES_NOT_EXIST;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;

@Service
@AllArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final UserDetailsService userDetailsService;
  private final ProjectService projectService;

  public List<Comment> getComments(Long projectId, String userId) {
    userDetailsService.checkIsUserApproved(userId);
    if (!projectService.exists(projectId)) {
      throw new ResourceNotFound(CommentService.class, PROJECT_DOES_NOT_EXIST);
    }

    return commentRepository.findByProjectId(projectId);
  }

  public Comment createComment(Comment comment, Long projectId, String loggedInUserId) {
    UserDetails author = userDetailsService.checkIsUserApproved(loggedInUserId);

    Project project =
        projectService
            .getProjectById(loggedInUserId, projectId)
            .orElseThrow(() -> new ResourceNotFound(CommentService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, projectId)));

    comment.setProject(project);
    comment.setAuthor(author);
    comment.setCreateDate(OffsetDateTime.now());
    return commentRepository.save(comment);
  }

  public Comment updateComment(
      Comment comment, Long commentId, String loggedInUserId, Long projectId) {

    userDetailsService.checkIsUserApproved(loggedInUserId);

    if (!projectService.exists(projectId)) {
      throw new ResourceNotFound(CommentService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, projectId));
    }

    Comment commentToEdit =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new ResourceNotFound(CommentService.class, COMMENT_NOT_FOUND, String.format(COMMENT_NOT_FOUND, commentId)));

    if (commentToEdit.hasEmptyOrDifferentAuthor(loggedInUserId)) {
      throw new ForbiddenException(CommentService.class, COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR,
          String.format(COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR, commentId));
    }

    commentToEdit.setText(comment.getText());
    return commentRepository.save(commentToEdit);
  }

  public boolean deleteComment(Long commentId, Long projectId, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    if (!projectService.exists(projectId)) {
      throw new ResourceNotFound(CommentService.class, PROJECT_DOES_NOT_EXIST);
    }

    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new ResourceNotFound(CommentService.class, COMMENT_NOT_FOUND, String.format(COMMENT_NOT_FOUND, commentId)));

    if (comment.hasEmptyOrDifferentAuthor(loggedInUserId)) {
      throw new ForbiddenException(CommentService.class, CANNOT_DELETE_COMMENT, String.format(CANNOT_DELETE_COMMENT, commentId));
    }

    try {
      commentRepository.deleteById(commentId);
    } catch (EmptyResultDataAccessException e) {
      throw new BadRequestException(EmptyResultDataAccessException.class, INVALID_COMMENT_ID, String.format("%s: %s", INVALID_COMMENT_ID, commentId));
    }
    return true;
  }
}
