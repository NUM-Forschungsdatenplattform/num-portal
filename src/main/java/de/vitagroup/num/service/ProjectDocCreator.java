package de.vitagroup.num.service;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.CohortGroup;
import de.vitagroup.num.domain.ProjectCategories;
import de.vitagroup.num.domain.Type;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.dto.TemplateInfoDto;
import de.vitagroup.num.domain.dto.UserDetailsDto;
import de.vitagroup.num.domain.repository.CohortRepository;
import de.vitagroup.num.web.exception.SystemException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProjectDocCreator {

  private final UserService userService;
  private final MessageSource messageSource;
  private final CohortRepository cohortRepository;

  public byte[] getDocBytesOfProject(ProjectDto projectInfo, Locale locale)
      throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
    writeDocument(projectInfo, outputStreamWriter, locale);
    outputStreamWriter.close();
    return outputStream.toByteArray();
  }

  private void writeDocument(@NotNull ProjectDto project, OutputStreamWriter outputStreamWriter,
      Locale locale)
      throws IOException {
    addTitle(project.getName(), outputStreamWriter);
    addSection("descrition", project.getDescription(), outputStreamWriter, locale);
    addSection("simple_description", project.getSimpleDescription(), outputStreamWriter, locale);
    addSection("goal", project.getGoal(), outputStreamWriter, locale);
    addSection("primary_hypothesis", project.getFirstHypotheses(), outputStreamWriter, locale);
    addSection("secodary_hypothesis", project.getSecondHypotheses(), outputStreamWriter, locale);
    addSection("status",
        messageSource.getMessage("status." + project.getStatus().toString().toLowerCase(
            Locale.ROOT), null, locale), outputStreamWriter, locale);
    addSection("lead",
        getUserAndOrgString(project.getCoordinator()),
        outputStreamWriter, locale);
    Set<String> keyWords = project.getKeywords();
    String keyWordsString = StringUtils.EMPTY;
    if (keyWords != null) {
      keyWordsString = String.join(", ", keyWords);
    }
    addSection("keywords", keyWordsString, outputStreamWriter, locale);
    Set<ProjectCategories> categories = project.getCategories();
    String categoriesString = StringUtils.EMPTY;
    if (categories != null) {
      categoriesString = categories.stream().map(category -> messageSource.getMessage(
              "category." + category.toString().toLowerCase(Locale.ROOT), null, locale))
          .collect(Collectors.joining(", "));
    }
    addSection("category", categoriesString, outputStreamWriter, locale);
    addSection("start_date",
        project.getStartDate() == null ? StringUtils.EMPTY : project.getStartDate().toString(),
        outputStreamWriter, locale);
    addSection("end_date", project.getEndDate() == null ? StringUtils.EMPTY : project.getEndDate().toString(),
        outputStreamWriter, locale);
    String researchers = getResearchers(project);
    addSection("researchers", researchers, outputStreamWriter, locale);
    addSection("private_financing", getYesNo(project.isFinanced(), locale), outputStreamWriter,
        locale);
    addSection("use_outside_eu", getYesNo(project.isUsedOutsideEu(), locale), outputStreamWriter,
        locale);
    String templatesString = StringUtils.EMPTY;
    List<TemplateInfoDto> templates = project.getTemplates();
    if (templates != null) {
      templatesString = templates.stream().map(TemplateInfoDto::getName)
          .collect(Collectors.joining("\n"));
    }
    addSection("templates", templatesString, outputStreamWriter, locale);
    addSection("cohort", getCohort(project), outputStreamWriter, locale);
  }

  private String getCohort(ProjectDto project) {
    Long cohortId = project.getCohortId();
    if (cohortId == null) {
      return StringUtils.EMPTY;
    }
    Cohort cohort = cohortRepository.findById(cohortId)
        .orElseThrow(() -> new SystemException("Can't find the cohort"));
    CohortGroup group = cohort.getCohortGroup();
    if (group == null) {
      return StringUtils.EMPTY;
    }
    StringBuilder builder = new StringBuilder();
    printCohortGroup(StringUtils.EMPTY, cohort.getCohortGroup(), builder);
    return builder.toString();
  }

  private void printCohortGroup(String prefix, CohortGroup cohortGroup, StringBuilder builder) {
    if (cohortGroup.getType() == Type.AQL) {
      builder.append(prefix);
      builder.append(cohortGroup.getQuery().getName());
      builder.append("   ");
      printParameters(cohortGroup.getParameters(), builder);
      builder.append(StringUtils.LF);
      return;
    }
    Set<CohortGroup> children = cohortGroup.getChildren();
    if (children == null) {
      return;
    }
    builder.append(prefix);
    builder.append(cohortGroup.getOperator().toString());
    builder.append(StringUtils.LF);
    children.forEach(group -> printCohortGroup(prefix + "  ", group, builder));
  }

  private void printParameters(Map<String, Object> parameters, StringBuilder builder) {
    if (parameters == null) {
      return;
    }
    builder.append(
        parameters.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining(", ")));
  }

  private String getYesNo(boolean yesNo, Locale locale) {
    if (yesNo) {
      return messageSource.getMessage("yes", null, locale);
    } else {
      return messageSource.getMessage("no", null, locale);
    }
  }

  private String getResearchers(ProjectDto project) {
    List<UserDetailsDto> researchers = project.getResearchers();
    if (researchers == null) {
      return StringUtils.EMPTY;
    }
    return researchers.stream().map(this::getResearcherString).collect(Collectors.joining("\n"));
  }

  private String getResearcherString(UserDetailsDto userDetailsDto) {
    User user = userService.getUserById(userDetailsDto.getUserId(), false);
    return getUserAndOrgString(user);
  }

  private String getUserAndOrgString(User user) {
    String userString = user.getFirstName() + " " + user.getLastName();
    if (user.getOrganization() == null) {
      return userString;
    }
    return userString + "   " + user.getOrganization().getName();
  }

  private void addTitle(String title, OutputStreamWriter outputStreamWriter) throws IOException {
    outputStreamWriter.append(StringUtils.LF);
    outputStreamWriter.append(title);
    outputStreamWriter.append(StringUtils.LF);
  }

  private void addSection(String headingKey, String text, OutputStreamWriter outputStreamWriter,
      Locale locale)
      throws IOException {
    String heading = messageSource.getMessage("project." + headingKey, null, locale);
    outputStreamWriter.append("\n\n\n");
    outputStreamWriter.append(heading);
    outputStreamWriter.append("\n\n");
    if(text == null){
      text = StringUtils.EMPTY;
    }
    outputStreamWriter.append(text);
  }
}
