package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.dto.CommentDto;
import de.vitagroup.num.service.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommentMapper {

  private final ModelMapper modelMapper;
  private final UserService userService;

  public CommentDto convertToDto(Comment comment) {
    CommentDto commentDto = modelMapper.map(comment, CommentDto.class);
    User author = userService.getOwner(comment.getAuthor().getUserId());
    commentDto.setAuthor(author);
    return commentDto;
  }

  public Comment convertToEntity(CommentDto commentDto) {
    Comment comment = modelMapper.map(commentDto, Comment.class);
    comment.setId(null);
    return comment;
  }
}
