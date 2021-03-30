package de.vitagroup.num.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.PhenotypeDto;
import de.vitagroup.num.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

@RunWith(MockitoJUnitRunner.class)
public class PhenotypeMapperTest {

  @Spy private ModelMapper modelMapper;

  @Mock
  private UserService userService;

  @InjectMocks private PhenotypeMapper phenotypeMapper;

  @Test
  public void shouldCorrectlyConvertToPhenotypeDto() {
    User user = User.builder().id("userId").build();
    when(userService.getUserById("userId", false)).thenReturn(user);

    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("phenotype")
            .description("phenotype description")
            .query(AqlExpression.builder().build())
            .owner(UserDetails.builder().userId("userId").build())
            .build();

    PhenotypeDto dto = phenotypeMapper.convertToDto(phenotype);

    assertThat(dto, notNullValue());
    assertThat(dto.getDescription(), is("phenotype description"));
    assertThat(dto.getName(), is("phenotype"));
    assertThat(dto.getId(), is(1L));
    assertThat(dto.getOwner(), notNullValue());
    assertThat(dto.getOwner(), is(user));
  }

  @Test
  public void shouldCorrectlyConvertToPhenotypeDtoWithoutOwner() {
    Phenotype phenotype =
        Phenotype.builder()
            .id(1L)
            .name("phenotype")
            .description("phenotype description")
            .query(AqlExpression.builder().build())
            .build();

    PhenotypeDto dto = phenotypeMapper.convertToDto(phenotype);

    assertThat(dto, notNullValue());
    assertThat(dto.getName(), is("phenotype"));
    assertThat(dto.getDescription(), is("phenotype description"));
    assertNull(dto.getOwner());
    assertThat(dto.getId(), is(1L));
  }
}
