package de.vitagroup.num.web.config;

public class Role {

  public static final String ADMIN = "hasRole('ADMIN')";
  public static final String ORGANIZATION_ADMIN = "hasRole('ORGANIZATION_ADMIN')";
  public static final String STUDY_COORDINATOR = "hasRole('STUDY_COORDINATOR')";
  public static final String RESEARCHER = "hasRole('RESEARCHER')";
  public static final String STUDY_COORDINATOR_AND_RESEARCHER =
      "hasAnyRole('STUDY_COORDINATOR', 'RESEARCHER')";

  private Role() {
  }
}
