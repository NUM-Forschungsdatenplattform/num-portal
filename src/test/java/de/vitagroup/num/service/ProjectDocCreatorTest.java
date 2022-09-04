package de.vitagroup.num.service;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CANNOT_DELETE_COMMENT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.CAN_T_FIND_THE_COHORT_BY_ID;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.COMMENT_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_COMMENTID_ID;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_DOES_NOT_EXIST;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.service.exception.SystemException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.EmptyResultDataAccessException;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.admin.UserDetails;
import de.vitagroup.num.domain.repository.CommentRepository;
import de.vitagroup.num.domain.repository.ProjectRepository;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDocCreatorTest {

  @Mock private UserDetailsService userDetailsService;

  @InjectMocks
  private ProjectDocCreator projectDocCreator;

  @Mock private CohortRepository cohortRepository;

  private ProjectDto projectDto;

  @Before
  public void setup() {
    projectDto = new ProjectDto();
    projectDto.setCohortId(1L);
    projectDto.setCreateDate(OffsetDateTime.now());
    projectDto.setId(1L);

    UserDetails approvedCoordinator =
            UserDetails.builder().userId("approvedCoordinatorId").approved(true).build();
  }

  @Test(expected = SystemException.class)
  public void findById() {
    when(cohortRepository.findById(2L))
            .thenThrow(new SystemException(ProjectDocCreator.class, CAN_T_FIND_THE_COHORT_BY_ID,
                            String.format(CAN_T_FIND_THE_COHORT_BY_ID, 2L)));
    cohortRepository.findById(2L);
  }

}
