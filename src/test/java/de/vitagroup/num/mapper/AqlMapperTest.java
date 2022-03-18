package de.vitagroup.num.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.service.UserService;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.time.OffsetDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

@RunWith(MockitoJUnitRunner.class)
public class AqlMapperTest {

  @Mock private AqlRepository aqlRepository;

  @Mock private UserDetailsRepository userDetailsRepository;

  @Mock private UserService userService;

  @Spy private ModelMapper modelMapper;

  @InjectMocks private AqlMapper mapper;

  @Before
  public void setup() {
    mapper.initialize();

    User user =
        User.builder()
            .id("123456")
            .organization(OrganizationDto.builder().name("organization a").id(1L).build())
            .build();

    when(userService.getOwner("123456")).thenReturn(user);
  }

  @Test
  public void shouldIgnoreMissingAqlOwnerInKeycloak() {
    when(userService.getOwner("missingUserId")).thenReturn(null);

    Aql aql = createAql();
    aql.setOwner(
        UserDetails.builder()
            .organization(Organization.builder().id(1L).build())
            .userId("missingUserId")
            .build());

    AqlDto dto = mapper.convertToDto(aql);

    assertThat(dto, notNullValue());
    assertThat(dto.getOwner(), nullValue());

    assertThat(dto.getName(), is(aql.getName()));
    assertThat(dto.getUse(), is(aql.getUse()));
    assertThat(dto.getPurpose(), is(aql.getPurpose()));
    assertThat(dto.getNameTranslated(), is(aql.getNameTranslated()));
    assertThat(dto.getUseTranslated(), is(aql.getUseTranslated()));
    assertThat(dto.getPurposeTranslated(), is(aql.getPurposeTranslated()));
    assertThat(dto.getCreateDate(), is(aql.getCreateDate()));
    assertThat(dto.getModifiedDate(), is(aql.getModifiedDate()));
    assertThat(dto.isPublicAql(), is(aql.isPublicAql()));
  }

  @Test
  public void shouldCorrectlyConvertAqlToDto() {

    Aql aql = createAql();
    aql.setOwner(
        UserDetails.builder()
            .organization(Organization.builder().id(1L).build())
            .userId("123456")
            .build());

    AqlDto dto = mapper.convertToDto(aql);

    assertThat(dto, notNullValue());
    assertThat(dto.getOwner(), notNullValue());
    assertThat(dto.getOwner().getId(), is(aql.getOwner().getUserId()));
    assertThat(dto.getOwner().getOrganization(), notNullValue());
    assertThat(
        dto.getOwner().getOrganization().getId(), is(aql.getOwner().getOrganization().getId()));

    assertThat(dto.getName(), is(aql.getName()));
    assertThat(dto.getUse(), is(aql.getUse()));
    assertThat(dto.getPurpose(), is(aql.getPurpose()));
    assertThat(dto.getNameTranslated(), is(aql.getNameTranslated()));
    assertThat(dto.getUseTranslated(), is(aql.getUseTranslated()));
    assertThat(dto.getPurposeTranslated(), is(aql.getPurposeTranslated()));
    assertThat(dto.getCreateDate(), is(aql.getCreateDate()));
    assertThat(dto.getModifiedDate(), is(aql.getModifiedDate()));
    assertThat(dto.isPublicAql(), is(aql.isPublicAql()));
  }

  @Test
  public void shouldCorrectlyConvertAqlDtoToEntity() {
    AqlDto dto =
        AqlDto.builder()
            .name("name")
            .use("use")
            .purpose("purpose")
            .nameTranslated("name - de")
            .useTranslated("use - de")
            .purposeTranslated("purpose -de ")
            .publicAql(false)
            .createDate(OffsetDateTime.now())
            .modifiedDate(OffsetDateTime.now())
            .build();

    Aql aql = mapper.convertToEntity(dto);

    assertThat(aql, notNullValue());
    assertThat(aql.getId(), nullValue());
    assertThat(aql.getName(), is(dto.getName()));
    assertThat(aql.getUse(), is(dto.getUse()));
    assertThat(aql.getPurpose(), is(dto.getPurpose()));
    assertThat(aql.getNameTranslated(), is(dto.getNameTranslated()));
    assertThat(aql.getUseTranslated(), is(dto.getUseTranslated()));
    assertThat(aql.getPurposeTranslated(), is(dto.getPurposeTranslated()));
    assertThat(aql.getCreateDate(), is(dto.getCreateDate()));
    assertThat(aql.getModifiedDate(), is(dto.getModifiedDate()));
    assertThat(aql.isPublicAql(), is(dto.isPublicAql()));
    assertThat(aql.getOwner(), nullValue());
  }

  private Aql createAql() {
    return Aql.builder()
        .name("name")
        .use("use")
        .purpose("purpose")
        .nameTranslated("name - de")
        .useTranslated("use - de")
        .purposeTranslated("purpose -de ")
        .publicAql(false)
        .createDate(OffsetDateTime.now())
        .modifiedDate(OffsetDateTime.now())
        .build();
  }
}
