/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Content;
import de.vitagroup.num.domain.ContentType;
import de.vitagroup.num.domain.dto.CardDto;
import de.vitagroup.num.domain.dto.CardDto.LocalizedPart;
import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.domain.repository.ContentItemRepository;
import java.net.MalformedURLException;
import java.net.URL;
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

  NavigationItemDto navigation1 =
      NavigationItemDto.builder().title("Link 11").url(new URL("https://www.google1.de")).build();
  NavigationItemDto navigation2 =
      NavigationItemDto.builder().title("Link 22").url(new URL("https://www.google2.de")).build();
  List<NavigationItemDto> navigations = List.of(navigation1, navigation2);
  String navigationJson =
      "[{\"title\":\"Link 11\",\"url\":\"https://www.google1.de\"},{\"title\":\"Link 22\",\"url\":\"https://www.google2.de\"}]";

  CardDto card1 =
      CardDto.builder()
          .en(LocalizedPart.builder().title("Title 1").text("text 1").build())
          .de(LocalizedPart.builder().title("Title de 1").text("text de 1").build())
          .imageId("image1")
          .url(new URL("http://test.test/"))
          .build();
  CardDto card2 =
      CardDto.builder()
          .en(LocalizedPart.builder().title("Title 2").text("text 2").build())
          .de(LocalizedPart.builder().title("Title de 2").text("text de 2").build())
          .imageId("image2")
          .url(new URL("http://test2.test/"))
          .build();
  List<CardDto> cards = List.of(card1, card2);
  String cardJson =
      "[{\"en\":{\"title\":\"Title 1\",\"text\":\"text 1\"},\"de\":{\"title\":\"Title de 1\",\"text\":\"text de 1\"},\"imageId\":\"image1\",\"url\":\"http://test.test/\"},{\"en\":{\"title\":\"Title 2\",\"text\":\"text 2\"},\"de\":{\"title\":\"Title de 2\",\"text\":\"text de 2\"},\"imageId\":\"image2\",\"url\":\"http://test2.test/\"}]";

  public ContentServiceTest() throws MalformedURLException {}

  @Test
  public void shouldCorrectlyRetrieveNoNavigation() {
    reset(repository);
    when(repository.findByType(ContentType.NAVIGATION)).thenReturn(Optional.of(new ArrayList<>()));
    assertEquals("[]", contentService.getNavigationItems());
  }

  @Test
  public void shouldCorrectlyRetrieveNavigation()
      throws MalformedURLException, JsonProcessingException {

    when(repository.findByType(ContentType.NAVIGATION))
        .thenReturn(
            Optional.of(
                List.of(
                    Content.builder()
                        .content(navigationJson)
                        .type(ContentType.NAVIGATION)
                        .id(1L)
                        .build())));

    List<NavigationItemDto> itemDtos =
        mapper.readValue(contentService.getNavigationItems(), new TypeReference<>() {});

    assertEquals(2, itemDtos.size());
    assertEquals("Link 11", itemDtos.get(0).getTitle());
    assertEquals("Link 22", itemDtos.get(1).getTitle());
    assertEquals(new URL("https://www.google1.de"), itemDtos.get(0).getUrl());
    assertEquals(new URL("https://www.google2.de"), itemDtos.get(1).getUrl());
  }

  @Test
  public void shouldCorrectlySaveNavigation() {
    when(repository.save(any())).thenReturn(null);

    contentService.setNavigationItems(navigations);

    ArgumentCaptor<Content> argumentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(repository).save(argumentCaptor.capture());
    Content capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.getType(), is(ContentType.NAVIGATION));
    assertEquals(navigationJson, capturedArgument.getContent());
  }

  @Test
  public void shouldCorrectlyRetrieveNoCards() {
    reset(repository);
    when(repository.findByType(ContentType.CARD)).thenReturn(Optional.of(new ArrayList<>()));
    assertEquals("[]", contentService.getCards());
  }

  @Test
  public void shouldCorrectlyRetrieveCards() throws MalformedURLException, JsonProcessingException {

    when(repository.findByType(ContentType.CARD))
        .thenReturn(
            Optional.of(
                List.of(
                    Content.builder().content(cardJson).type(ContentType.CARD).id(1L).build())));

    String json = contentService.getCards();
    List<CardDto> itemDtos = mapper.readValue(json, new TypeReference<>() {});

    assertEquals(2, itemDtos.size());
    assertEquals("Title 1", itemDtos.get(0).getEn().getTitle());
    assertEquals("Title 2", itemDtos.get(1).getEn().getTitle());
    assertEquals(new URL("http://test.test/"), itemDtos.get(0).getUrl());
    assertEquals(new URL("http://test2.test/"), itemDtos.get(1).getUrl());
    reset(repository);
  }

  @Test
  public void shouldCorrectlySaveCards() {
    when(repository.save(any())).thenReturn(null);

    contentService.setCards(cards);

    ArgumentCaptor<Content> argumentCaptor = ArgumentCaptor.forClass(Content.class);
    verify(repository).save(argumentCaptor.capture());
    Content capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument.getType(), is(ContentType.CARD));
    assertEquals(cardJson, capturedArgument.getContent());
  }
}
