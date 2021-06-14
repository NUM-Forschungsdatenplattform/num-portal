package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.dto.CardDto;
import de.vitagroup.num.domain.dto.MetricsDto;
import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.domain.dto.ProjectInfoDto;
import de.vitagroup.num.service.ContentService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Validated
@Controller
@AllArgsConstructor
@RequestMapping(value = "/content", produces = "application/json")
public class ContentController {

  private final ContentService contentService;

  @GetMapping("/navigation")
  @ApiOperation(value = "Retrieves a list of navigation items")
  @ApiResponses({
    @ApiResponse(code = 200, message = "OK"),
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public ResponseEntity<String> getNavigationItems() {
    return ResponseEntity.ok(contentService.getNavigationItems());
  }

  @GetMapping("/metrics")
  @ApiOperation(value = "Retrieves platform metrics")
  public ResponseEntity<MetricsDto> getMetrics() {
    return ResponseEntity.ok(contentService.getMetrics());
  }

  @GetMapping("/graph/clinic")
  @ApiOperation(value = "Retrieves the list of participating clinics")
  public ResponseEntity<List<String>> getClinics() {
    return ResponseEntity.ok(contentService.getClinics());
  }

  @GetMapping("/graph/clinic/{name}/sofaDistribution")
  @ApiOperation(value = "Retrieves sofa distribution of a clinic")
  public ResponseEntity<Map<String, Integer>> getClinicDistributions(@PathVariable String name) {
    return ResponseEntity.ok(contentService.getClinicDistributions(name));
  }

  @GetMapping("/graph/clinic/sofaAverage")
  @ApiOperation(value = "Retrieves the sofa averages of participating clinics")
  public ResponseEntity<Map<String, Double>> getClinicAverages() {
    return ResponseEntity.ok(contentService.getClinicAverages());
  }

  @GetMapping("/latest-projects")
  @ApiOperation(value = "Retrieves latest project info")
  public ResponseEntity<List<ProjectInfoDto>> getLatestProjects() {
    return ResponseEntity.ok(contentService.getLatestProjects());
  }

  @PostMapping("/navigation")
  @ApiOperation(value = "Retrieves a list of navigation items")
  @PreAuthorize(Role.CONTENT_ADMIN)
  @ApiResponses({
    @ApiResponse(code = 200, message = "OK", response = String.class),
    @ApiResponse(code = 400, message = "Bad request"),
    @ApiResponse(code = 401, message = "Unauthorized"),
    @ApiResponse(code = 403, message = "Forbidden"),
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public ResponseEntity<String> setNavigationItems(
      @Valid @NotNull @RequestBody @Size(max = 5) List<NavigationItemDto> navigationItemDtos) {
    contentService.setNavigationItems(navigationItemDtos);
    return ResponseEntity.ok("Success");
  }

  @GetMapping("/cards")
  @ApiOperation(value = "Retrieves a list of cards")
  @ApiResponses({
    @ApiResponse(code = 200, message = "OK"),
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public ResponseEntity<String> getCards() {
    return ResponseEntity.ok(contentService.getCards());
  }

  @PostMapping("/cards")
  @ApiOperation(value = "Retrieves a list of cards")
  @PreAuthorize(Role.CONTENT_ADMIN)
  @ApiResponses({
    @ApiResponse(code = 200, message = "OK", response = String.class),
    @ApiResponse(code = 400, message = "Bad request"),
    @ApiResponse(code = 401, message = "Unauthorized"),
    @ApiResponse(code = 403, message = "Forbidden"),
    @ApiResponse(code = 500, message = "Internal server error")
  })
  public ResponseEntity<String> setCards(
      @Valid @NotNull @RequestBody @Size(max = 8) List<CardDto> cardDtos) {
    contentService.setCards(cardDtos);
    return ResponseEntity.ok("Success");
  }
}
