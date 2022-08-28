package de.vitagroup.num.service;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_DELETE_COMMENT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COMMENT_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_COMMENTID_ID;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_DOES_NOT_EXIST;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.CommentDto;
import de.vitagroup.num.service.exception.ResourceNotFound;
import org.springframework.dao.EmptyResultDataAccessException;

@RunWith(MockitoJUnitRunner.class)
public class CommentServiceTest {

  @Mock private UserDetailsService userDetailsService;

  @Mock private CommentService commentService;

  private Comment comment;

  @Before
  public void setup() {
    comment = new Comment();
    comment.setAuthor(UserDetails.builder().userId("4").build());
    comment.setCreateDate(OffsetDateTime.now());
    comment.setId(1L);

    when(commentService.getComments(1L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_DOES_NOT_EXIST));

    when(commentService.createComment(comment, 1L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, 1L)));

    when(commentService.updateComment(comment, 1L, "userId", 1L))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, 1L)));

    when(commentService.updateComment(comment, 2L, "userId", 1L))
            .thenThrow(new ResourceNotFound(CommentService.class, COMMENT_NOT_FOUND, String.format(COMMENT_NOT_FOUND, 2L)));

    when(commentService.updateComment(comment, 3L, "userId", 1L))
            .thenThrow( new ForbiddenException(CommentService.class, COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR,
            String.format(COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR, 3L)));

    when(commentService.deleteComment(1L, 1L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_DOES_NOT_EXIST));

    when(commentService.deleteComment(2L, 1L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, COMMENT_NOT_FOUND, String.format(COMMENT_NOT_FOUND, 2L)));

    when(commentService.deleteComment(3L, 1L, "userId"))
            .thenThrow(new ForbiddenException(CommentService.class, CANNOT_DELETE_COMMENT, String.format(CANNOT_DELETE_COMMENT, 3L)));

    when(commentService.deleteComment(4L, 1L, "userId"))
            .thenThrow(new BadRequestException(EmptyResultDataAccessException.class, INVALID_COMMENTID_ID, String.format("%s: %s", INVALID_COMMENTID_ID, 4L)));

  }

  @Test(expected = ResourceNotFound.class)
  public void getCommentsForNonExistingProject() {
   commentService.getComments(1L, "userId");
  }

  @Test(expected = ResourceNotFound.class)
  public void createCommentForNonExistingProject() {
    commentService.createComment(comment, 1L, "userId");
  }

  @Test(expected = ResourceNotFound.class)
  public void updateCommentForNonExistingProject() {
    commentService.updateComment(comment, 1L, "userId", 1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void updateCommentForNonExistingCommentInProject() {
    commentService.updateComment(comment, 2L, "userId", 1L);
  }

  @Test(expected = ForbiddenException.class)
  public void updateCommentBelongToAnotherAuthor() {
    commentService.updateComment(comment, 3L, "userId", 1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteCommentForNonExistingProject() {
    commentService.deleteComment(1L, 1L, "userId");
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteCommentIdForExistingProjectNotFound() {
    commentService.deleteComment(2L, 1L, "userId");
  }

  @Test(expected = ForbiddenException.class)
  public void deleteCommentIsEmptyOrBelongToAnotherAuthor() {
    commentService.deleteComment(3L, 1L, "userId");
  }

  @Test(expected = BadRequestException.class)
  public void deleteCommentInvalidCommentId() {
    commentService.deleteComment(4L, 1L, "userId");
  }

}
