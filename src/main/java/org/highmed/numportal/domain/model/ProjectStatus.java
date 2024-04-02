package org.highmed.numportal.domain.model;

import static org.highmed.numportal.domain.model.Roles.STUDY_APPROVER;
import static org.highmed.numportal.domain.model.Roles.STUDY_COORDINATOR;
import static org.highmed.numportal.domain.model.Roles.SUPER_ADMIN;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ProjectStatus {
  /** Project creation in progress */
  DRAFT {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          DRAFT, List.of(STUDY_COORDINATOR),
          PENDING, List.of(STUDY_COORDINATOR));
    }
  },

  /** Project is finalized and send for approval but still editable */
  PENDING {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          PENDING, List.of(STUDY_COORDINATOR),
          REVIEWING, List.of(STUDY_APPROVER),
          DRAFT, List.of(STUDY_COORDINATOR));
    }
  },

  /** Project is being review and cannot be edited */
  REVIEWING {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          REVIEWING, List.of(STUDY_APPROVER),
          APPROVED, List.of(STUDY_APPROVER),
          CHANGE_REQUEST, List.of(STUDY_APPROVER),
          DENIED, List.of(STUDY_APPROVER));
    }
  },

  /** Pending requests from the reviewer */
  CHANGE_REQUEST {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          CHANGE_REQUEST, List.of(STUDY_COORDINATOR),
          DRAFT, List.of(STUDY_COORDINATOR),
          PENDING, List.of(STUDY_COORDINATOR));
    }
  },

  /** Project is denied */
  DENIED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          DENIED, List.of(STUDY_COORDINATOR),
          ARCHIVED, List.of(STUDY_COORDINATOR, SUPER_ADMIN));
    }
  },

  /** Project is approved */
  APPROVED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          APPROVED, List.of(STUDY_COORDINATOR),
          PUBLISHED, List.of(STUDY_COORDINATOR));
    }
  },

  /** Project is published and cannot be edited anymore */
  PUBLISHED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          PUBLISHED, List.of(STUDY_COORDINATOR),
          CLOSED, List.of(STUDY_COORDINATOR));
    }
  },

  /** Project is finished */
  CLOSED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          CLOSED, List.of(STUDY_COORDINATOR),
          ARCHIVED, List.of(STUDY_COORDINATOR, SUPER_ADMIN));
    }
  },

  /** Project is archived */
  ARCHIVED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of();
    }
  };

  public abstract Map<ProjectStatus, List<String>> nextStatusesAndRoles();

  public static List<ProjectStatus> getAllProjectStatusToViewAsCoordinator() {
    return Arrays.asList(ProjectStatus.APPROVED, ProjectStatus.PUBLISHED, ProjectStatus.CLOSED);
  }

  public static List<ProjectStatus> getAllProjectStatusToViewAsResearcher() {
    return List.of(ProjectStatus.PUBLISHED);
  }

  public static List<ProjectStatus> getAllProjectStatusToViewAsApprover() {
    return Stream.of(ProjectStatus.values())
            .filter(
                    projectStatus ->
                            projectStatus != ProjectStatus.DRAFT
                            && projectStatus != ProjectStatus.ARCHIVED)
            .collect(Collectors.toList());
  }
}
