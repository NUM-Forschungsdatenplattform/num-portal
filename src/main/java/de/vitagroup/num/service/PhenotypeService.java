package de.vitagroup.num.service;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlExpression;
import de.vitagroup.num.domain.Expression;
import de.vitagroup.num.domain.GroupExpression;
import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.PhenotypeRepository;
import de.vitagroup.num.service.executors.PhenotypeExecutor;
import de.vitagroup.num.web.exception.BadRequestException;
import de.vitagroup.num.web.exception.ForbiddenException;
import de.vitagroup.num.web.exception.SystemException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PhenotypeService {

  private final PhenotypeRepository phenotypeRepository;
  private final UserDetailsService userDetailsService;
  private final AqlService aqlService;
  private final PhenotypeExecutor phenotypeExecutor;

  public Optional<Phenotype> getPhenotypeById(Long phenotypeId) {
    return phenotypeRepository.findById(phenotypeId);
  }

  public List<Phenotype> getAllPhenotypes(String loggedInUserId) {
    UserDetails owner =
        userDetailsService.getUserDetailsById(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Logged in owner not approved.");
    }

    return phenotypeRepository.findByOwnerUserId(loggedInUserId);
  }

  public Phenotype createPhenotypes(Phenotype phenotype, String loggedInUserId) {

    UserDetails owner =
        userDetailsService.getUserDetailsById(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Logged in owner not approved.");
    }

    validatePhenotypeAqls(phenotype, loggedInUserId);

    phenotype.setOwner(owner);
    return phenotypeRepository.save(phenotype);
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

        if (aql.isEmpty() || (!aql.get().isViewable(loggedInUserId)))  {
          throw new BadRequestException(
              "One of the phenotype aqls cannot be found in the num portal or access to it is forbidden");
        }

      } else if (current instanceof GroupExpression) {
        queue.addAll(((GroupExpression) current).getChildren());
      }
    }
  }

  public Set<String> executePhenotype(Phenotype phenotype, String loggedInUserId) {
    UserDetails owner =
        userDetailsService.getUserDetailsById(loggedInUserId).orElseThrow(SystemException::new);

    if (owner.isNotApproved()) {
      throw new ForbiddenException("Logged in owner not approved.");
    }

    return phenotypeExecutor.execute(phenotype);
  }

  public long getPhenotypeSize(Phenotype phenotype, String loggedInUserId) {
    return executePhenotype(phenotype, loggedInUserId).size();
  }
}
