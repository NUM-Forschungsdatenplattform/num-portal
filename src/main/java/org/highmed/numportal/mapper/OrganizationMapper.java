package org.highmed.numportal.mapper;

import org.highmed.numportal.domain.dto.OrganizationDto;
import org.highmed.numportal.domain.model.MailDomain;
import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.service.OrganizationService;

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
