package de.vitagroup.num.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.dto.AqlSearchFilter;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.service.ehrbase.EhrBaseService;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ehrbase.response.openehr.QueryResponseData;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AqlService {

  private final AqlRepository aqlRepository;

  private final UserDetailsRepository userDetailsRepository;

  private final EhrBaseService ehrBaseService;

  private final ObjectMapper mapper;

  public Optional<Aql> getAqlById(Long id) {
    return aqlRepository.findById(id);
  }

  public Aql getAqlById(Long id, String loggedInUserId) {
    UserDetails owner =
        userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }

    Aql aql = aqlRepository.findById(id).orElseThrow(ResourceNotFound::new);

    if (aql.isViewable(loggedInUserId)) {
      return aql;
    } else {
      throw new ForbiddenException("Cannot access this aql.");
    }
  }

  public List<Aql> getVisibleAqls(String loggedInUserId) {
    UserDetails owner =
        userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }
    return aqlRepository.findAllOwnedOrPublic(owner.getUserId());
  }

  public Aql createAql(Aql aql, String loggedInUserId) {
    UserDetails owner =
        userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }

    aql.setOwner(owner);
    aql.setCreateDate(OffsetDateTime.now());
    aql.setModifiedDate(OffsetDateTime.now());

    return aqlRepository.save(aql);
  }

  public Aql updateAql(Aql aql, Long aqlId, String loggedInUserId) {
    UserDetails owner =
        userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }

    Aql aqlToEdit = aqlRepository.findById(aqlId).orElseThrow(ResourceNotFound::new);

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
    UserDetails owner =
        userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }

    Aql aql = aqlRepository.findById(id).orElseThrow(ResourceNotFound::new);

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
   * @param loggedInUserId
   * @return
   */
  public List<Aql> searchAqls(String name, AqlSearchFilter filter, String loggedInUserId) {

    UserDetails owner =
        userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }

    switch (filter) {
      case ALL:
        return aqlRepository.findAllOwnedOrPublicByName(owner.getUserId(), name);
      case OWNED:
        return aqlRepository.findAllOwnedByName(owner.getUserId(), name);
      case ORGANIZATION:
        return aqlRepository.findAllOrganizationOwnedByName(
            owner.getOrganizationId(), owner.getUserId(), name);
      default:
        return List.of();
    }
  }

  public String executeAql(Long aqlId, String userId) {
    UserDetails owner =
        userDetailsRepository.findByUserId(userId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }

    Aql aql = aqlRepository.findById(aqlId).orElseThrow(ResourceNotFound::new);

    if (aql.isExecutable(userId)) {

      try {
        QueryResponseData response = ehrBaseService.executeAql(aql);
        return mapper.writeValueAsString(response);
      } catch (JsonProcessingException e) {
        throw new SystemException("An issue has occurred, cannot execute aql.");
      }

    } else {
      throw new ForbiddenException("Cannot access this resource.");
    }
  }
}
