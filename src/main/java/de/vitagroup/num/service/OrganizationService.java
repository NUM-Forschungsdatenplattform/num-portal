package de.vitagroup.num.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.web.exception.ResourceNotFound;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Service responsible for retrieving organization information from the terminology server */
@Slf4j
@Service
@AllArgsConstructor
public class OrganizationService {

  private final ObjectMapper mapper;

  /**
   * Retrieves a list with available organizations
   *
   * @return List with available organizations
   */
  public List<OrganizationDto> getAllOrganizations() {
    return getOrganizations();
  }

  /**
   * Retrieves an organization by external identifier
   *
   * @param id
   * @return
   */
  public OrganizationDto getOrganizationById(String id) {
    Optional<OrganizationDto> organizationDto = findOrganizationById(id);

    if (organizationDto.isEmpty()) {
      throw new ResourceNotFound("Organization not found");
    }
    return organizationDto.get();
  }

  // TODO: Implement call to the terminology server responsible with organization data
  private Optional<OrganizationDto> findOrganizationById(String id) {
    return getOrganizations().stream()
        .filter(organizationDto -> organizationDto.getId().equals(id))
        .findFirst();
  }

  // TODO: Implement call to the terminology server responsible with organization data
  private List<OrganizationDto> getOrganizations() {
    File organizationFile =
        new File(
            getClass()
                .getClassLoader()
                .getResource("mock/organization/organizations.json")
                .getFile());
    try {

      String content = FileUtils.readFileToString(organizationFile, StandardCharsets.UTF_8);

      return mapper.readValue(content, new TypeReference<List<OrganizationDto>>() {});

    } catch (IOException e) {
      log.error("Error reading mock organization from file");
    }
    return Collections.emptyList();
  }
}
