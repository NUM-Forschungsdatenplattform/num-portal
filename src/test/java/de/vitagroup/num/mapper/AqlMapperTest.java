package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

@RunWith(MockitoJUnitRunner.class)
public class AqlMapperTest {

  @Mock private AqlRepository aqlRepository;

  @Mock private UserDetailsRepository userDetailsRepository;

  @Spy private ModelMapper modelMapper;

  @InjectMocks private AqlMapper mapper;

  @Before
  public void setup() {
    mapper.initialize();
  }

  @Test
  public void shouldCorrectlyConvertAqlToDto() {
    Aql aql =
        Aql.builder()
            .name("name")
            .use("use")
            .purpose("purpose")
            .publicAql(false)
            .createDate(OffsetDateTime.now())
            .modifiedDate(OffsetDateTime.now())
            .organizationId("abcd")
            .build();

    AqlDto dto = mapper.convertToDto(aql);

    assertThat(dto, notNullValue());

    assertThat(dto.getName(), is(aql.getName()));
    assertThat(dto.getUse(), is(aql.getUse()));
    assertThat(dto.getPurpose(), is(aql.getPurpose()));
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
            .publicAql(false)
            .ownerId("ownerId")
            .createDate(OffsetDateTime.now())
            .modifiedDate(OffsetDateTime.now())
            .organizationId("abcd")
            .build();

    Aql aql = mapper.convertToEntity(dto);

    assertThat(aql, notNullValue());
    assertThat(aql.getId(), nullValue());
    assertThat(aql.getName(), is(dto.getName()));
    assertThat(aql.getUse(), is(dto.getUse()));
    assertThat(aql.getPurpose(), is(dto.getPurpose()));
    assertThat(aql.getCreateDate(), is(dto.getCreateDate()));
    assertThat(aql.getModifiedDate(), is(dto.getModifiedDate()));
    assertThat(aql.isPublicAql(), is(dto.isPublicAql()));
    assertThat(aql.getOwner(), nullValue());
  }
}
