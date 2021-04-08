package de.vitagroup.num.service;

import static java.lang.String.format;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.CohortGroupRepository;
import de.vitagroup.num.domain.repository.PhenotypeRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.executors.PhenotypeExecutor;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.PrivacyException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.ehrbase.aql.parser.AqlParseException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PhenotypeService {

  private final CohortGroupRepository cohortGroupRepository;
  private final PrivacyProperties privacyProperties;
  private final PhenotypeRepository phenotypeRepository;
  private final UserDetailsService userDetailsService;
  private final AqlService aqlService;
  private final PhenotypeExecutor phenotypeExecutor;

  public boolean isPhenotypeNotInUse(long phenotypeId) {
    if (phenotypeRepository.existsById(phenotypeId)) {
      return !cohortGroupRepository.existsByPhenotypeId(phenotypeId);
    } else {
      throw new ResourceNotFound(format("Phenotype not found: %s", phenotypeId));
    }
  }

  public void deletePhenotypeById(Long id, String userId, List<String> roles) {
    userDetailsService.checkIsUserApproved(userId);

    Phenotype phenotype =
        phenotypeRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFound("Phenotype not found: " + id));

    if (phenotype.hasEmptyOrDifferentOwner(userId) && !roles.contains(Roles.SUPER_ADMIN)) {
      throw new ForbiddenException("Cannot delete phenotype: " + id);
    }

    if (isPhenotypeNotInUse(id)) {
      try {
        phenotypeRepository.deleteById(id);
      } catch (EmptyResultDataAccessException e) {
        throw new BadRequestException(String.format("%s: %s", "Invalid phenotype id", id));
      }
    } else {
      phenotype.setDeleted(true);
      phenotypeRepository.save(phenotype);
    }
  }

  public Phenotype getPhenotypeById(Long phenotypeId, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    return phenotypeRepository
        .findById(phenotypeId)
        .orElseThrow(() -> new ResourceNotFound("Phenotype not found: " + phenotypeId));
  }

  public Optional<Phenotype> getPhenotypeById(Long phenotypeId) {
    return phenotypeRepository.findById(phenotypeId);
  }

  public List<Phenotype> getAllPhenotypes(String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    return phenotypeRepository.findByDeletedFalse();
  }

  public Phenotype createPhenotypes(Phenotype phenotype, String loggedInUserId) {

    UserDetails user = userDetailsService.checkIsUserApproved(loggedInUserId);

    validatePhenotypeAqls(phenotype, loggedInUserId);
    phenotype.setDeleted(false);
    phenotype.setOwner(user);
    return phenotypeRepository.save(phenotype);
  }

  public long getPhenotypeSize(Phenotype phenotype, String loggedInUserId) {
    userDetailsService.checkIsUserApproved(loggedInUserId);

    Set<String> ehrIds;
    try {
      ehrIds = phenotypeExecutor.execute(phenotype);
    } catch (AqlParseException e) {
      throw new BadRequestException(e.getMessage());
    }

    if (ehrIds.size() < privacyProperties.getMinHits()) {
      throw new PrivacyException("Too few matches, results withheld for privacy reasons.");
    }
    return ehrIds.size();
  }

  private void validatePhenotypeAqls(Phenotype phenotype, String loggedInUserId) {
    Expression aqlExpression = phenotype.getQuery();

    if (aqlExpression == null) {
      throw new BadRequestException("Empty phenotype");
    }

    Queue<Expression> queue = new ArrayDeque<>();
    queue.add(aqlExpression);

    while (!queue.isEmpty()) {
      Expression current = queue.remove();

      if (current instanceof AqlExpression) {

        Optional<Aql> aql = aqlService.getAqlById(((AqlExpression) current).getAql().getId());

        if (aql.isEmpty() || (!aql.get().isViewable(loggedInUserId))) {
          throw new BadRequestException(
              "One of the phenotype aqls cannot be found in the num portal or access to it is forbidden");
        }

      } else if (current instanceof GroupExpression) {
        queue.addAll(((GroupExpression) current).getChildren());
      }
    }
  }
}
