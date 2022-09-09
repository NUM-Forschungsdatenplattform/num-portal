package de.vitagroup.num.service;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_DELETE_COMMENT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COMMENT_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_COMMENTID_ID;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_DOES_NOT_EXIST;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.EmptyResultDataAccessException;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.CommentRepository;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;

@RunWith(MockitoJUnitRunner.class)
public class CommentServiceTest {

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks
  private CommentService commentService;

  @Mock private ProjectService projectService;

  @Mock private CommentRepository commentRepository;

  @Mock
  private ProjectRepository projectRepository;

  private Comment comment;

  @Before
  public void setup() {
    comment = new Comment();
    comment.setAuthor(UserDetails.builder().userId("4").build());
    comment.setCreateDate(OffsetDateTime.now());
    comment.setId(1L);

    UserDetails approvedCoordinator =
            UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();

    when(commentRepository.findById(3L))
            .thenReturn(
                    Optional.of(
                            Comment.builder()
                                    .id(3L)
                                    .author(approvedCoordinator)
                                    .build()));

  }

  @Test(expected = ResourceNotFound.class)
  public void getCommentsForNonExistingProject() {
    when(commentService.getComments(2L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_DOES_NOT_EXIST));
    ResourceNotFound exception =
            assertThrows(
                    ResourceNotFound.class,
                    () -> commentService.getComments(2L, "userId"));
    assertThat(exception.getMessage(), is(PROJECT_DOES_NOT_EXIST));
  }

  @Test(expected = ResourceNotFound.class)
  public void createCommentForNonExistingProject() {
    when(commentService.createComment(comment, 1L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, 1L)));
    commentService.createComment(comment, 1L, "userId");
  }

  @Test(expected = ResourceNotFound.class)
  public void updateCommentForNonExistingProject() {
    when(commentService.updateComment(comment, 1L, "userId", 1L))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, 1L)));
    commentService.updateComment(comment, 1L, "userId", 1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void updateCommentForNonExistingCommentInProject() {
    when(commentService.updateComment(comment, 2L, "userId", 1L))
            .thenThrow(new ResourceNotFound(CommentService.class, COMMENT_NOT_FOUND, String.format(COMMENT_NOT_FOUND, 2L)));
    commentService.updateComment(comment, 2L, "userId", 1L);
  }

  @Test(expected = ForbiddenException.class)
  public void updateCommentBelongToAnotherAuthor() {
    when(projectService.exists(1L))
            .thenReturn(true);
    when(commentService.updateComment(comment, 3L, "userId", 1L))
            .thenThrow( new ForbiddenException(CommentService.class, COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR,
                    String.format(COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR, 3L)));
    commentService.updateComment(comment, 3L, "userId", 1L);
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteCommentForNonExistingProject() {
    when(commentService.deleteComment(1L, 1L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, PROJECT_DOES_NOT_EXIST));
    commentService.deleteComment(1L, 1L, "userId");
  }

  @Test(expected = ResourceNotFound.class)
  public void deleteCommentIdForExistingProjectNotFound() {
    when(commentService.deleteComment(2L, 1L, "userId"))
            .thenThrow(new ResourceNotFound(CommentService.class, COMMENT_NOT_FOUND, String.format(COMMENT_NOT_FOUND, 2L)));
    commentService.deleteComment(2L, 1L, "userId");
  }

  @Test(expected = ForbiddenException.class)
  public void deleteCommentIsEmptyOrBelongToAnotherAuthor() {
    when(projectService.exists(1L))
            .thenReturn(true);
    when(commentService.deleteComment(3L, 1L, "userId"))
            .thenThrow(new ForbiddenException(CommentService.class, CANNOT_DELETE_COMMENT, String.format(CANNOT_DELETE_COMMENT, 3L)));
    commentService.deleteComment(3L, 1L, "userId");
  }

  @Test(expected = BadRequestException.class)
  public void deleteCommentInvalidCommentId() {
    when(projectService.exists(1L))
            .thenReturn(true);
    when(commentService.deleteComment(3L, 1L, "approvedCoordinatorId"))
            .thenThrow(new BadRequestException(EmptyResultDataAccessException.class, INVALID_COMMENTID_ID, String.format("%s: %s", INVALID_COMMENTID_ID, 3L)));
    commentService.deleteComment(3L, 1L, "approvedCoordinatorId");
  }

}