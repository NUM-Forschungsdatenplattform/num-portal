package de.vitagroup.num.mapper;

import de.vitagroup.num.domain.model.MailDomain;
import de.vitagroup.num.domain.model.Organization;
import de.vitagroup.num.domain.dto.OrganizationDto;
import java.util.stream.Collectors;

import de.vitagroup.num.service.OrganizationService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrganizationMapper {

  private final ModelMapper modelMapper;

  private final OrganizationService organizationService;

  public OrganizationDto convertToDto(Organization organization) {
    OrganizationDto organizationDto = modelMapper.map(organization, OrganizationDto.class);
    organizationDto.setMailDomains(
        organization.getDomains().stream()
            .map(MailDomain::getName)
            .collect(Collectors.toSet()));
    organizationDto.setAllowedToBeDeleted(organizationService.isAllowedToBeDeleted(organization.getId()));

    return organizationDto;
  }
}
