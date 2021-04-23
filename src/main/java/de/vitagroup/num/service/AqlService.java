package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.AqlSearchFilter;
import de.vitagroup.num.domain.dto.SlimAqlDto;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.PrivacyException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.aql.parser.AqlParseException;
import org.ehrbase.aqleditor.dto.aql.QueryValidationResponse;
import org.ehrbase.aqleditor.dto.aql.Result;
import org.ehrbase.aqleditor.service.AqlEditorAqlService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AqlService {

  private final AqlRepository aqlRepository;

  private final EhrBaseService ehrBaseService;

  private final ObjectMapper mapper;

  private final UserDetailsService userDetailsService;

  private final PrivacyProperties privacyProperties;

  private final AqlEditorAqlService aqlEditorAqlService;

  /**
   * Counts the number of aql queries existing in the platform
   *
   * @return
   */
  public long countAqls() {
    return aqlRepository.count();
  }

  public Optional<Aql> getAqlById(Long id) {
    return aqlRepository.findById(id);
  }

  public Aql getAqlById(Long id, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    Aql aql =
        aqlRepository.findById(id).orElseThrow(() -> new ResourceNotFound("Aql not found: " + id));

    if (aql.isViewable(loggedInUserId)) {
      return aql;
    } else {
      throw new ForbiddenException("Cannot access this aql.");
    }
  }

  public List<Aql> getVisibleAqls(String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);
    return aqlRepository.findAllOwnedOrPublic(loggedInUserId);
  }

  public Aql createAql(Aql aql, String loggedInUserId) {
    UserDetails user = userDetailsService.checkIsUserApproved(loggedInUserId);

    if (user.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in user not approved.");
    }

    aql.setOwner(user);
    aql.setCreateDate(OffsetDateTime.now());
    aql.setModifiedDate(OffsetDateTime.now());

    return aqlRepository.save(aql);
  }

  public Aql updateAql(Aql aql, Long aqlId, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    Aql aqlToEdit =
        aqlRepository
            .findById(aqlId)
            .orElseThrow(() -> new ResourceNotFound("Cannot find aql: " + aqlId));

    if (aqlToEdit.hasEmptyOrDifferentOwner(loggedInUserId)) {
      throw new ForbiddenException(
          String.format(
              "%s: %s %s.",
              "Aql edit for aql with id", aqlId, "not allowed. Aql has different owner"));
    }

    aqlToEdit.setName(aql.getName());
    aqlToEdit.setPurpose(aql.getPurpose());
    aqlToEdit.setUse(aql.getUse());
    aqlToEdit.setModifiedDate(OffsetDateTime.now());
    aqlToEdit.setQuery(aql.getQuery());
    aqlToEdit.setPublicAql(aql.isPublicAql());

    return aqlRepository.save(aqlToEdit);
  }

  public void deleteById(Long id, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    Aql aql =
        aqlRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound("Cannot find aql: " + id));

    if (aql.hasEmptyOrDifferentOwner(loggedInUserId)) {
      throw new ForbiddenException("Cannot delete aql: " + id);
    }

    try {
      aqlRepository.deleteById(id);
    } catch (EmptyResultDataAccessException e) {
      throw new BadRequestException(String.format("%s: %s", "Invalid aql id", id));
    }
  }

  /**
   * Searches among a list of visible AQLs.
   *
   * @param name A string contained in the name of the aqls
   * @param filter Type of the search. Search all owned or public, searched owned only or search
   *     among own organization
   * @param loggedInUserId the user ID of the user sending the search request
   * @return the list of AQLs that match the search filters
   */
  public List<Aql> searchAqls(String name, AqlSearchFilter filter, String loggedInUserId) {

    UserDetails user = userDetailsService.checkIsUserApproved(loggedInUserId);

    if (user.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in user not approved.");
    }

    switch (filter) {
      case ALL:
        return aqlRepository.findAllOwnedOrPublicByName(user.getUserId(), name);
      case OWNED:
        return aqlRepository.findAllOwnedByName(user.getUserId(), name);
      case ORGANIZATION:
        return aqlRepository.findAllOrganizationOwnedByName(
            user.getOrganization().getId(), user.getUserId(), name);
      default:
        return List.of();
    }
  }

  public long getAqlSize(SlimAqlDto aql, String userId) {
    userDetailsService.checkIsUserApproved(userId);

    validateQuery(aql.getQuery());

    Set<String> ehrIds;
    try {
      ehrIds =
          ehrBaseService.retrieveEligiblePatientIds(Aql.builder().query(aql.getQuery()).build());
    } catch (AqlParseException e) {
      throw new BadRequestException(e.getMessage());
    }

    if (ehrIds.size() < privacyProperties.getMinHits()) {
      throw new PrivacyException("Too few matches, results withheld for privacy reasons.");
    }
    return ehrIds.size();
  }

  private void validateQuery(String query) {
    QueryValidationResponse response =
        aqlEditorAqlService.validateAql(Result.builder().q(query).build());
    if (!response.isValid()) {
      try {
        throw new BadRequestException(mapper.writeValueAsString(response));
      } catch (JsonProcessingException e) {
        log.error("Could not serialize aql validation response", e);
      }
    }
  }
}
