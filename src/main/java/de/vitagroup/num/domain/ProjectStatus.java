package de.vitagroup.num.domain;

import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;
import static de.vitagroup.num.domain.Roles.SUPER_ADMIN;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum ProjectStatus {
  /** Project creation in progress */
  DRAFT {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          DRAFT, Arrays.asList(STUDY_COORDINATOR),
          PENDING, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Project is finalized and send for approval but still editable */
  PENDING {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          PENDING, Arrays.asList(STUDY_COORDINATOR),
          REVIEWING, Arrays.asList(STUDY_APPROVER),
          DRAFT, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Project is being review and cannot be edited */
  REVIEWING {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          REVIEWING, Arrays.asList(STUDY_APPROVER),
          APPROVED, Arrays.asList(STUDY_APPROVER),
          CHANGE_REQUEST, Arrays.asList(STUDY_APPROVER),
          DENIED, Arrays.asList(STUDY_APPROVER));
    }
  },

  /** Pending requests from the reviewer */
  CHANGE_REQUEST {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          CHANGE_REQUEST, Arrays.asList(STUDY_COORDINATOR),
          DRAFT, Arrays.asList(STUDY_COORDINATOR),
          PENDING, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Project is denied */
  DENIED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          DENIED, Arrays.asList(STUDY_COORDINATOR),
          ARCHIVED, Arrays.asList(STUDY_COORDINATOR, SUPER_ADMIN));
    }
  },

  /** Project is approved */
  APPROVED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          APPROVED, Arrays.asList(STUDY_COORDINATOR),
          PUBLISHED, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Project is published and cannot be edited anymore */
  PUBLISHED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          PUBLISHED, Arrays.asList(STUDY_COORDINATOR),
          CLOSED, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Project is finished */
  CLOSED {
    @Override
    public Map<ProjectStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          CLOSED, Arrays.asList(STUDY_COORDINATOR),
          ARCHIVED, Arrays.asList(STUDY_COORDINATOR, SUPER_ADMIN));
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
}
