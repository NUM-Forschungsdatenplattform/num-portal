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

  public void writeDocument(@NotNull ProjectDto project, OutputStreamWriter outputStreamWriter,
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
    String keyWordsString = "";
    if (keyWords != null) {
      keyWordsString = String.join(", ", keyWords);
    }
    addSection("keywords", keyWordsString, outputStreamWriter, locale);
    Set<ProjectCategories> categories = project.getCategories();
    String categoriesString = "";
    if (categories != null) {
      categoriesString = categories.stream().map(category -> messageSource.getMessage(
              "category." + category.toString().toLowerCase(Locale.ROOT), null, locale))
          .collect(Collectors.joining(", "));
    }
    addSection("category", categoriesString, outputStreamWriter, locale);
    addSection("start_date",
        project.getStartDate() == null ? "" : project.getStartDate().toString(),
        outputStreamWriter, locale);
    addSection("end_date", project.getEndDate() == null ? "" : project.getEndDate().toString(),
        outputStreamWriter, locale);
    String researchers = getResearchers(project);
    addSection("researchers", researchers, outputStreamWriter, locale);
    addSection("private_financing", getYesNo(project.isFinanced(), locale), outputStreamWriter,
        locale);
    addSection("use_outside_eu", getYesNo(project.isUsedOutsideEu(), locale), outputStreamWriter,
        locale);
    String templatesString = "";
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
      return "";
    }
    Cohort cohort = cohortRepository.findById(cohortId)
        .orElseThrow(() -> new SystemException("Can't find the cohort"));
    CohortGroup group = cohort.getCohortGroup();
    if (group == null) {
      return "";
    }
    StringBuffer buffer = new StringBuffer();
    printCohortGroup("", cohort.getCohortGroup(), buffer);
    return buffer.toString();
  }

  private void printCohortGroup(String prefix, CohortGroup cohortGroup, StringBuffer buffer) {
    if (cohortGroup.getType() == Type.AQL) {
      buffer.append(prefix);
      buffer.append(cohortGroup.getQuery().getName());
      buffer.append("   ");
      printParameters(cohortGroup.getParameters(), buffer);
      buffer.append("\n");
      return;
    }
    Set<CohortGroup> children = cohortGroup.getChildren();
    if (children == null) {
      return;
    }
    buffer.append(prefix);
    buffer.append(cohortGroup.getOperator().toString());
    buffer.append("\n");
    children.forEach(group -> printCohortGroup(prefix + "  ", group, buffer));
  }

  private void printParameters(Map<String, Object> parameters, StringBuffer buffer) {
    if (parameters == null) {
      return;
    }
    buffer.append(
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
      return "";
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
    outputStreamWriter.append("\n");
    outputStreamWriter.append(title);
    outputStreamWriter.append("\n");
  }

  private void addSection(String headingKey, String text, OutputStreamWriter outputStreamWriter,
      Locale locale)
      throws IOException {
    String heading = messageSource.getMessage("project." + headingKey, null, locale);
    outputStreamWriter.append("\n\n\n");
    outputStreamWriter.append(heading);
    outputStreamWriter.append("\n\n");
    outputStreamWriter.append(text);
  }
}
