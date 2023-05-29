package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.MailDomain;
import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.dto.OrganizationDto;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(MockitoJUnitRunner.class)
public class OrganizationMapperTest {

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private OrganizationMapper organizationMapper;

    @Test
    public void shouldConvertEntityToDto() {
        Organization entity = Organization.builder()
                .id(2L)
                .name("organization name")
                .domains(Set.of(MailDomain.builder().name("vitagroup.ag").build()))
                .build();
        OrganizationDto dto = organizationMapper.convertToDto(entity);
        assertThat(dto, notNullValue());
        assertThat(dto.getId(), is(entity.getId()));
        Assert.assertTrue(dto.getMailDomains().contains("vitagroup.ag"));
    }
}
