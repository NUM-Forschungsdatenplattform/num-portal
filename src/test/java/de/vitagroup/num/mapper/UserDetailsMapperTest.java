package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsMapperTest {

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private UserDetailsMapper userDetailsMapper;

    @Before
    public void setup() {
        userDetailsMapper.initialize();
    }

    @Test
    public void shouldConvertDtoToEntity() {
        UserDetailsDto dto = UserDetailsDto.builder()
                .userId("userId")
                .organizationId("3")
                .approved(Boolean.TRUE)
                .build();
        UserDetails entity = userDetailsMapper.convertToEntity(dto);
        assertThat(entity, notNullValue());
        assertThat(entity.getUserId(), is(dto.getUserId()));
        assertThat(entity.getOrganization(), notNullValue());
        assertThat(entity.isApproved(), is(dto.getApproved()));
    }

    @Test
    public void shouldConvertEntityToDto() {
        UserDetails entity = UserDetails
                .builder()
                .userId("userId")
                .organization(Organization.builder()
                        .id(1L)
                        .name("organization name")
                        .build())
                .build();
        UserDetailsDto dto = userDetailsMapper.convertToDto(entity);
        assertThat(dto, notNullValue());
        assertThat(dto.getUserId(), notNullValue());
        assertThat(dto.getApproved(), is(entity.isApproved()));
        assertThat(dto.getOrganizationId(), notNullValue());
        assertThat(dto.getOrganizationId(), is(entity.getOrganization().getId().toString()));
    }
}
