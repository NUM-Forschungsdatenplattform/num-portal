package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.dto.OrganizationDto;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrganizationMapper {

  private final ModelMapper modelMapper;

  public OrganizationDto convertToDto(Organization organization) {
    OrganizationDto organizationDto = modelMapper.map(organization, OrganizationDto.class);
    organizationDto.setMailDomains(
        organization.getDomains().stream()
            .map(domain -> domain.getName())
            .collect(Collectors.toSet()));

    return organizationDto;
  }
}
