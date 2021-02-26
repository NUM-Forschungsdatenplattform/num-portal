package de.vitagroup.num.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.vitagroup.num.service.UserDetailsService;
import de.vitagroup.num.service.UserService;
import de.vitagroup.num.web.controller.AdminController;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.RestExceptionHandler;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class AdminControllerTest {

  private MockMvc mvc;

  @Mock UserService userService;

  @Mock UserDetailsService userDetailsService;

  @InjectMocks
  AdminController adminController;

  @BeforeEach
  public void setup() {
    mvc =
        MockMvcBuilders.standaloneSetup(adminController)
            .setControllerAdvice(new RestExceptionHandler())
            .build();
  }

  @Test
  void shouldFailUpdateInvalidRole() throws Exception {
    when(userService.setUserRoles("1", Collections.singletonList("test")))
        .thenThrow(
            new BadRequestException(
                "Unknown Role(s): " + String.join(" ", Collections.singletonList("test"))));
    mvc.perform(
            post("/admin/user/1/role")
                .contentType("application/json")
                .content("[\"test\"]")
                .accept("application/json"))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void shouldUpdateRoles() throws Exception {
    when(userService.setUserRoles("1", Collections.singletonList("ADMIN")))
        .thenReturn(Collections.singletonList("ADMIN"));
    mvc.perform(
            post("/admin/user/1/role")
                .contentType("application/json")
                .content("[\"ADMIN\"]")
                .accept("application/json"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string("[\"ADMIN\"]"));
  }
}
