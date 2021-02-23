package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.PhenotypeRepository;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.service.executors.PhenotypeExecutor;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.PrivacyException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.ehrbase.aql.parser.AqlParseException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PhenotypeService {

  private final PrivacyProperties privacyProperties;
  private final PhenotypeRepository phenotypeRepository;
  private final UserDetailsService userDetailsService;
  private final AqlService aqlService;
  private final PhenotypeExecutor phenotypeExecutor;

  public Optional<Phenotype> getPhenotypeById(Long phenotypeId) {
    return phenotypeRepository.findById(phenotypeId);
  }

  public List<Phenotype> getAllPhenotypes(String loggedInUserId) {
    validateLoggedInUser(loggedInUserId);

    return phenotypeRepository.findByOwnerUserId(loggedInUserId);
  }

  public Phenotype createPhenotypes(Phenotype phenotype, String loggedInUserId) {

    UserDetails user =
        userDetailsService
            .getUserDetailsById(loggedInUserId)
            .orElseThrow(() -> new SystemException("Logged in user not found"));

    if (user.isNotApproved()) {
      throw new ForbiddenException("Logged in owner not approved.");
    }

    validatePhenotypeAqls(phenotype, loggedInUserId);

    phenotype.setOwner(user);
    return phenotypeRepository.save(phenotype);
  }

  public long getPhenotypeSize(Phenotype phenotype, String loggedInUserId) {
    validateLoggedInUser(loggedInUserId);

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

  public void validateLoggedInUser(String userId) {
    UserDetails user =
        userDetailsService
            .getUserDetailsById(userId)
            .orElseThrow(() -> new SystemException("Logged in user not found"));

    if (user.isNotApproved()) {
      throw new ForbiddenException("Logged in user is not approved.");
    }
  }
}
