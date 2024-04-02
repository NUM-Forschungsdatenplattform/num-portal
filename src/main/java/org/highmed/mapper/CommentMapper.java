package org.highmed.mapper;

import org.highmed.domain.dto.CommentDto;
import org.highmed.domain.model.Comment;
import org.highmed.domain.model.admin.User;
import org.highmed.service.UserService;
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
