package org.highmed.numportal.web.config;

public class Role {
  public static final String SUPER_ADMIN = "hasRole('SUPER_ADMIN')";
  public static final String ORGANIZATION_ADMIN = "hasRole('ORGANIZATION_ADMIN')";
  public static final String MANAGER = "hasRole('MANAGER')";
  public static final String STUDY_COORDINATOR = "hasRole('STUDY_COORDINATOR')";
  public static final String STUDY_APPROVER = "hasRole('STUDY_APPROVER')";
  public static final String RESEARCHER = "hasRole('RESEARCHER')";
  public static final String CRITERIA_EDITOR = "hasRole('CRITERIA_EDITOR')";
  public static final String CONTENT_ADMIN = "hasRole('CONTENT_ADMIN')";
  public static final String MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER =
      "hasAnyRole('MANAGER', 'STUDY_COORDINATOR', 'RESEARCHER')";
  public static final String MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR =
          "hasAnyRole('MANAGER', 'STUDY_COORDINATOR', 'RESEARCHER', 'CRITERIA_EDITOR')";

  public static final String STUDY_COORDINATOR_OR_APPROVER =
      "hasAnyRole('STUDY_COORDINATOR', 'STUDY_APPROVER')";
  public static final String CRITERIA_EDITOR_OR_SUPER_ADMIN = "hasAnyRole('CRITERIA_EDITOR', 'SUPER_ADMIN')";
  public static final String MANAGER_OR_STUDY_COORDINATOR =
      "hasAnyRole('MANAGER', 'STUDY_COORDINATOR')";
  public static final String STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER =
      "hasAnyRole('STUDY_COORDINATOR', 'RESEARCHER', 'STUDY_APPROVER')";
  public static final String MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER =
      "hasAnyRole('MANAGER', 'STUDY_COORDINATOR', 'RESEARCHER', 'STUDY_APPROVER')";
  public static final String STUDY_COORDINATOR_OR_SUPER_ADMIN =
      "hasAnyRole('SUPER_ADMIN', 'STUDY_COORDINATOR')";
  public static final String SUPER_ADMIN_OR_ORGANIZATION_ADMIN =
      "hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN')";
  public static final String SUPER_ADMIN_OR_ORGANIZATION_ADMIN_OR_STUDY_COORDINATOR =
      "hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'STUDY_COORDINATOR')";

  private Role() {}
}
