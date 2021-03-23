package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.service.UserService;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/profile", produces = "application/json")
@AllArgsConstructor
public class ProfileController {

  private final UserService userService;

  @GetMapping()
  @ApiOperation(value = "Retrieves the user profile information")
  public ResponseEntity<User> getUserProfile(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(userService.getUserProfile(principal.getSubject()));
  }
}
