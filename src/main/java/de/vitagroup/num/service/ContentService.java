package de.vitagroup.num.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Content;
import de.vitagroup.num.domain.ContentType;
import de.vitagroup.num.domain.dto.CardDto;
import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.domain.repository.ContentItemRepository;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ContentService {

  private final ContentItemRepository contentItemRepository;
  private final ObjectMapper mapper;

  public String getNavigationItems() {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.NAVIGATION).orElse(new ArrayList<>());
    if (contents.isEmpty()) {
      return "[]";
    } else {
      try {
        return contents.get(0).getContent();
      } catch (Exception e) {
        log.error("Couldn't parse navigation content", e);
        throw new SystemException("Couldn't parse navigation content", e);
      }
    }
  }

  public void setNavigationItems(List<NavigationItemDto> navigationItemDtos) {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.NAVIGATION).orElse(new ArrayList<>());
    Content navigation;
    if (contents.isEmpty()) {
      navigation = Content.builder().type(ContentType.NAVIGATION).build();
    } else {
      navigation = contents.get(0);
    }

    try {
      navigation.setContent(mapper.writeValueAsString(navigationItemDtos));
      contentItemRepository.save(navigation);
    } catch (Exception e) {
      log.error("Couldn't save navigation content", e);
      throw new SystemException("Couldn't save navigation content", e);
    }
  }

  public String getCards() {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.CARD).orElse(new ArrayList<>());
    if (contents.isEmpty()) {
      return "[]";
    } else {
      try {
        return contents.get(0).getContent();
      } catch (Exception e) {
        log.error("Couldn't parse card", e);
        throw new SystemException("Couldn't parse card", e);
      }
    }
  }

  public void setCards(List<CardDto> cardDtos) {
    List<Content> contents =
        contentItemRepository.findByType(ContentType.CARD).orElse(new ArrayList<>());
    Content navigation;
    if (contents.isEmpty()) {
      navigation = Content.builder().type(ContentType.CARD).build();
    } else {
      navigation = contents.get(0);
    }

    try {
      navigation.setContent(mapper.writeValueAsString(cardDtos));
      contentItemRepository.save(navigation);
    } catch (Exception e) {
      log.error("Couldn't save card", e);
      throw new SystemException("Couldn't save card", e);
    }
  }
}
