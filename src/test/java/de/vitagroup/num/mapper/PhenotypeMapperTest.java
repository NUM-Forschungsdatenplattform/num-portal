package de.vitagroup.num.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.PhenotypeDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

@RunWith(MockitoJUnitRunner.class)
public class PhenotypeMapperTest {

  @Spy private ModelMapper modelMapper;

  @InjectMocks private PhenotypeMapper phenotypeMapper;

  @Test
  public void shouldCorrectlyConvertToPhenotypeDto() {
    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("testname")
            .description("test desc")
            .query(AqlExpression.builder().build())
            .owner(UserDetails.builder().userId("test user id").build())
            .build();

    PhenotypeDto dto = phenotypeMapper.convertToDto(phenotype);

    assertThat(dto, notNullValue());
    assertThat(dto.getDescription(), is("test desc"));
    assertThat(dto.getName(), is("testname"));
    assertThat(dto.getOwnerId(), is("test user id"));
    assertThat(dto.getId(), is(1L));
  }

  @Test
  public void shouldCorrectlyConvertToPhenotypeDtoWithoutOwner() {
    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("testname")
            .description("test desc")
            .query(AqlExpression.builder().build())
            .build();

    PhenotypeDto dto = phenotypeMapper.convertToDto(phenotype);

    assertThat(dto, notNullValue());
    assertThat(dto.getDescription(), is("test desc"));
    assertThat(dto.getName(), is("testname"));
    assertNull(dto.getOwnerId());
    assertThat(dto.getId(), is(1L));
  }
}
