package de.vitagroup.num.web.controller;

import static de.vitagroup.num.domain.Roles.RESEARCHER;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.AQL_NOT_FOUND;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.integrationtesting.security.WithMockNumUser;
import de.vitagroup.num.mapper.AqlMapper;
import de.vitagroup.num.security.WithMockJwt;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.service.ehrbase.ParameterService;
import de.vitagroup.num.service.exception.ResourceNotFound;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("it")
@WebMvcTest(controllers = {AqlController.class})
@AutoConfigureMockMvc
public class AqlControllerTest {
  public static final String UNAUTHORIZED_USER_ID = "b59e5edb-3121-4e0a-8ccb-af6798207a73";
  private static final String AQL_PATH = "/aql";
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AqlService aqlService;

  @MockBean
  private ParameterService parameterService;

  @MockBean
  private AqlMapper aqlMapper;

  @MockBean
  private ModelMapper modelMapper;

  @BeforeEach
  void reset() {
    Mockito.reset(aqlService);
  }

  @Test
  @SneakyThrows
  @WithMockUser(roles = {SUPER_ADMIN})
  void shouldNotAccessAqlApiWithWrongRole() {
    mockMvc.perform(get(String.format("%s/%s", AQL_PATH, 1))).andExpect(status().isForbidden());
  }

  @Test
  @SneakyThrows
  @WithMockNumUser(
      roles = {RESEARCHER},
      expiredToken = true)
  void shouldHandleExpiredToken() {
    mockMvc.perform(get(String.format("%s/%s", AQL_PATH, 1))).andExpect(status().isUnauthorized());
  }

  @Test
  @SneakyThrows
  @WithMockJwt(realmAccessRoles = RESEARCHER)
  void shouldReturnNotFound() {
    Long resourceId = 16577756L;
    ResourceNotFound ex = new ResourceNotFound(AqlService.class, AQL_NOT_FOUND, String.format(AQL_NOT_FOUND, resourceId));
    doThrow(ex).when(aqlService).getAqlById(Mockito.anyLong(), Mockito.anyString());

    mockMvc
        .perform(get(String.format("%s/%s", AQL_PATH, resourceId)))
        .andExpect(status().isNotFound());

    verify(aqlService, Mockito.atLeastOnce()).getAqlById(Mockito.anyLong(), Mockito.anyString());
  }

  @Test
  @SneakyThrows
  @WithMockJwt(realmAccessRoles = RESEARCHER)
  //@WithMockNumUser(
  //    userId = UNAUTHORIZED_USER_ID,
  //    roles = {RESEARCHER})
  public void shouldHandleNotApprovedUserWhenSavingAql() {

    Aql aql =
        Aql.builder()
            .name("t1")
            .use("use")
            .purpose("purpose")
            .nameTranslated("t1_de")
            .purposeTranslated("purpose_de")
            .useTranslated("use_de")
            .query("t3")
            .publicAql(true)
            .build();
    String aqlJson = objectMapper.writeValueAsString(aql);

    mockMvc
        .perform(
            post(AQL_PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(aqlJson))
        .andExpect(status().isForbidden());
  }
}
