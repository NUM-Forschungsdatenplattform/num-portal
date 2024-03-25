package org.highmed.mapper;

import org.highmed.domain.dto.OrganizationDto;
import org.highmed.domain.model.MailDomain;
import org.highmed.domain.model.Organization;
import org.highmed.service.OrganizationService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

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
