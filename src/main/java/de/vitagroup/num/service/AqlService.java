package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import de.vitagroup.num.web.exception.SystemException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AqlService {

  private final AqlRepository aqlRepository;

  private final UserDetailsRepository userDetailsRepository;

  public Optional<Aql> getAqlById(Long id) {
    return aqlRepository.findById(id);
  }

  public List<Aql> getAllAqls() {
    return aqlRepository.findAll();
  }

  public Aql createAql(Aql aql, String loggedInUserId) {
    UserDetails owner = userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Cannot access this resource. Logged in owner not approved.");
    }

    aql.setOwner(owner);
    aql.setCreateDate(OffsetDateTime.now());
    aql.setModifiedDate(OffsetDateTime.now());

    return aqlRepository.save(aql);
  }

  public Aql updateAql(Aql aql, Long aqlId, String loggedInUserId) {
    UserDetails owner = userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

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
    aqlToEdit.setDescription(aql.getDescription());
    aqlToEdit.setModifiedDate(OffsetDateTime.now());
    aqlToEdit.setQuery(aql.getQuery());
    aqlToEdit.setOrganizationId(aql.getOrganizationId());
    aqlToEdit.setPublicAql(aql.isPublicAql());

    return aqlRepository.save(aqlToEdit);
  }

  public void deleteById(Long id, String loggedInUserId) {
    UserDetails owner = userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

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
   * Retrieves a list of AQLs. If no parameter is specified retrieves all the aqls
   *
   * @param name A string contained in the name of the aqls
   * @param owned Flag filtering aqls by owner
   * @param ownedBySameOrganization Flag filtering aqls by owner organization
   * @param loggedInUserId
   * @return
   */
  public List<Aql> searchAqls(
      String name, Boolean owned, Boolean ownedBySameOrganization, String loggedInUserId) {

    UserDetails owner =
        userDetailsRepository.findByUserId(loggedInUserId).orElseThrow(SystemException::new);

    if (StringUtils.isEmpty(name)
        && ObjectUtils.isEmpty(owned)
        && ObjectUtils.isEmpty(ownedBySameOrganization)) {

      return aqlRepository.findAll();

    } else {

      return aqlRepository.findAqlByNameAndOrganizationAndOwner(
          name,
          BooleanUtils.isTrue(ownedBySameOrganization) ? owner.getOrganizationId() : null,
          BooleanUtils.isTrue(owned) ? owner.getUserId() : null);
    }
  }
}
