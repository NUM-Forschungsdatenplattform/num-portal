package de.vitagroup.num.domain;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

public class Roles {
  public static final String SUPER_ADMIN = "SUPER_ADMIN";
  public static final String CONTENT_ADMIN = "CONTENT_ADMIN";
  public static final String ORGANIZATION_ADMIN = "ORGANIZATION_ADMIN";
  public static final String MANAGER = "MANAGER";
  public static final String STUDY_COORDINATOR = "STUDY_COORDINATOR";
  public static final String STUDY_APPROVER = "STUDY_APPROVER";
  public static final String RESEARCHER = "RESEARCHER";
  public static final String CRITERIA_EDITOR = "CRITERIA_EDITOR";

  private static final List<String> SUPER_ADMIN_ASSIGNABLE =
      Lists.newArrayList(
          SUPER_ADMIN,
          CONTENT_ADMIN,
          ORGANIZATION_ADMIN,
          MANAGER,
          STUDY_COORDINATOR,
          STUDY_APPROVER,
          RESEARCHER,
          CRITERIA_EDITOR);
  private static final List<String> ORGANIZATION_ADMIN_ASSIGNABLE =
      Lists.newArrayList(ORGANIZATION_ADMIN, STUDY_COORDINATOR, RESEARCHER);

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";

  private Roles() {}

  public static List<String> extractRoles(Jwt principal) {
    Map<String, Object> access = principal.getClaimAsMap(REALM_ACCESS_CLAIM);
    if (access == null) {
      return new ArrayList<>();
    }
    return Arrays.asList((String[])access.get(ROLES_CLAIM));
  }

  public static boolean isAllowedToSet(String roleToSet, List<String> callersRoles) {
    if (callersRoles.contains(SUPER_ADMIN) && SUPER_ADMIN_ASSIGNABLE.contains(roleToSet)) {
      return true;
    }
    return callersRoles.contains(ORGANIZATION_ADMIN)
        && ORGANIZATION_ADMIN_ASSIGNABLE.contains(roleToSet);
  }

  public static boolean hasRole(String role, Jwt principal) {
    return extractRoles(principal).contains(role);
  }

  public static boolean isSuperAdmin(List<String> roles) {
    return roles.contains(Roles.SUPER_ADMIN);
  }

  public static boolean isOrganizationAdmin(List<String> roles) {
    return roles.contains(Roles.ORGANIZATION_ADMIN);
  }

  public static boolean isProjectLead(List<String> roles) {
    return roles.contains(Roles.STUDY_COORDINATOR);
  }


}
