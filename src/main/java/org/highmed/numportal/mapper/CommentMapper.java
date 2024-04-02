package org.highmed.numportal.mapper;

import org.highmed.numportal.domain.dto.CommentDto;
import org.highmed.numportal.domain.model.Comment;
import org.highmed.numportal.domain.model.admin.User;
import org.highmed.numportal.service.UserService;
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
