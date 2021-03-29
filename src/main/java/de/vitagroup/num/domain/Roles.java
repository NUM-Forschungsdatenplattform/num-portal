/*
 * Copyright 2021. Vitagroup AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.vitagroup.num.domain;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

public class Roles {
  public static final String SUPER_ADMIN = "SUPER_ADMIN";
  public static final String CONTENT_ADMIN = "CONTENT_ADMIN";
  public static final String ORGANIZATION_ADMIN = "ORGANIZATION_ADMIN";
  public static final String STUDY_COORDINATOR = "STUDY_COORDINATOR";
  public static final String STUDY_APPROVER = "STUDY_APPROVER";
  public static final String RESEARCHER = "RESEARCHER";

  public static final List<String> SUPER_ADMIN_ASSIGNABLE =
      Lists.newArrayList(
          SUPER_ADMIN,
          CONTENT_ADMIN,
          ORGANIZATION_ADMIN,
          STUDY_COORDINATOR,
          STUDY_APPROVER,
          RESEARCHER);
  public static final List<String> ORGANIZATION_ADMIN_ASSIGNABLE =
      Lists.newArrayList(ORGANIZATION_ADMIN, STUDY_COORDINATOR, STUDY_APPROVER, RESEARCHER);

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";

  private Roles() {}

  public static List<String> extractRoles(Jwt principal) {
    Map<String, Object> access = principal.getClaimAsMap(REALM_ACCESS_CLAIM);
    return (List<String>) access.get(ROLES_CLAIM);
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
}
