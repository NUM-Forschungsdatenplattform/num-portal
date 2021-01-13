package de.vitagroup.num.web.config;

public class Role {

  public static final String SUPER_ADMIN = "hasRole('SUPER_ADMIN')";
  public static final String ORGANIZATION_ADMIN = "hasRole('ORGANIZATION_ADMIN')";
  public static final String STUDY_COORDINATOR = "hasRole('STUDY_COORDINATOR')";
  public static final String RESEARCHER = "hasRole('RESEARCHER')";
  public static final String STUDY_COORDINATOR_OR_RESEARCHER =
      "hasAnyRole('STUDY_COORDINATOR', 'RESEARCHER')";
  public static final String STUDY_COORDINATOR_OR_SUPER_ADMIN =
      "hasAnyRole('STUDY_COORDINATOR', 'SUPER_ADMIN')";

  private Role() {
  }
}
