package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.AqlRepository;
import de.vitagroup.num.domain.repository.UserDetailsRepository;
import de.vitagroup.num.web.exception.NotAuthorizedException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    Optional<UserDetails> owner = userDetailsRepository.findByUserId(loggedInUserId);

    if (owner.isEmpty()) {
      throw new NotAuthorizedException("Logged in owner not found in portal");
    }

    if (owner.get().isNotApproved()) {
      throw new NotAuthorizedException("Logged in owner not approved:");
    }

    aql.setOwner(owner.get());
    aql.setCreateDate(OffsetDateTime.now());
    aql.setModifiedDate(OffsetDateTime.now());

    return aqlRepository.save(aql);
  }

  public Aql updateAql(Aql aql, Long aqlId, String loggedInUserId) {
    Aql aqlToEdit = aqlRepository.findById(aqlId).orElseThrow(ResourceNotFound::new);

    if (!aqlToEdit.getOwner().getUserId().equals(loggedInUserId)) {
      throw new NotAuthorizedException(
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
}
