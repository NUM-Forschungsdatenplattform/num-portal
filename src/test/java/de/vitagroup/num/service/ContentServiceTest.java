package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Content;
import de.vitagroup.num.domain.ContentType;
import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.domain.repository.ContentItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContentServiceTest {

  @Spy private ObjectMapper mapper;

  @Spy private ContentItemRepository repository;

  @InjectMocks private ContentService contentService;

  NavigationItemDto item1 =
      NavigationItemDto.builder().title("Link 11").url("https://www.google1.de").build();
  NavigationItemDto item2 =
      NavigationItemDto.builder().title("Link 22").url("https://www.google2.de").build();
  List<NavigationItemDto> items = List.of(item1, item2);

  String json =
      "[{\"title\":\"Link 11\",\"url\":\"https://www.google1.de\"},{\"title\":\"Link 22\",\"url\":\"https://www.google2.de\"}]";

  @Test
  public void shouldCorrectlyRetrieveNoNavigation() {

    when(repository.findByType(any())).thenReturn(Optional.of(new ArrayList<>()));
    assertTrue(contentService.getNavigationItems().isEmpty());
  }

  @Test
  public void shouldCorrectlyRetrieveNavigation() {

    when(repository.findByType(any()))
        .thenReturn(
            Optional.of(
                List.of(
                    Content.builder().content(json).type(ContentType.NAVIGATION).id(1L).build())));
    List<NavigationItemDto> itemDtos = contentService.getNavigationItems();
    assertEquals(2, itemDtos.size());
    assertEquals("Link 11", itemDtos.get(0).getTitle());
    assertEquals("Link 22", itemDtos.get(1).getTitle());
    assertEquals("https://www.google1.de", itemDtos.get(0).getUrl());
    assertEquals("https://www.google2.de", itemDtos.get(1).getUrl());
  }

  @Test
  public void shouldCorrectlySaveNavigation() {
    when(repository.save(any())).thenReturn(null);
    contentService.setNavigationItems(items);
    ArgumentCaptor<Content> argumentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(repository).save(argumentCaptor.capture());
    Content capturedArgument = argumentCaptor.<Content>getValue();
    assertThat(capturedArgument.getType(), is(ContentType.NAVIGATION));
    assertEquals(json, capturedArgument.getContent());
  }
}
