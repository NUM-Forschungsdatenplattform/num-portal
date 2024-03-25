package org.highmed.mapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import org.highmed.domain.dto.CommentDto;
import org.highmed.domain.model.Comment;
import org.highmed.domain.model.Project;
import org.highmed.domain.model.admin.User;
import org.highmed.domain.model.admin.UserDetails;
import org.highmed.service.UserService;

import java.time.OffsetDateTime;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

@RunWith(MockitoJUnitRunner.class)
public class CommentMapperTest {

    @Spy
    private ModelMapper modelMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentMapper commentMapper;

    @Test
    public void shouldConvertEntityToDto() {
        Comment entity = Comment.builder()
                .text("dummy comment text")
                .project(Project.builder()
                        .id(9L)
                        .build())
                .createDate(OffsetDateTime.now())
                .build();
        entity.setAuthor(UserDetails.builder()
                        .userId("userId-123")
                .build());
        Mockito.when(userService.getOwner("userId-123"))
                .thenReturn(User.builder()
                        .id("userId-123")
                        .firstName("John")
                        .lastName("Doe")
                        .build());
        CommentDto dto = commentMapper.convertToDto(entity);
        assertThat(dto, notNullValue());
        assertThat(dto.getAuthor(), notNullValue());
        assertThat(dto.getText(), is(entity.getText()));
        assertThat(dto.getProjectId(), is(entity.getProject().getId()));
        assertThat(dto.getCreateDate(), is(entity.getCreateDate()));
    }

    @Test
    public void shouldConvertDtoToEntity () {
        CommentDto dto = CommentDto.builder()
                .projectId(9L)
                .text("some comment text")
                .build();
        Comment entity = commentMapper.convertToEntity(dto);
        assertThat(entity, notNullValue());
        assertThat(entity.getId(), nullValue());
        assertThat(entity.getProject(), notNullValue());
        assertThat(entity.getText(), is(dto.getText()));
    }

}
