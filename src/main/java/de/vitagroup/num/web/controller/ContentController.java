package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.service.ContentService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Validated
@Controller
@AllArgsConstructor
@RequestMapping("/content")
public class ContentController {

  private final ContentService contentService;

  @GetMapping("/navigation")
  @ApiOperation(value = "Retrieves a list of navigation items")
  public ResponseEntity<List<NavigationItemDto>> getNavigationItems() {
    return ResponseEntity.ok(contentService.getNavigationItems());
  }

  @PostMapping("/navigation")
  @ApiOperation(value = "Retrieves a list of navigation items")
  @PreAuthorize(Role.CONTENT_ADMIN)
  public ResponseEntity<String> setNavigationItems(
      @Valid @NotNull @RequestBody List<NavigationItemDto> navigationItemDtos) {
    contentService.setNavigationItems(navigationItemDtos);
    return ResponseEntity.ok("Success");
  }
}
