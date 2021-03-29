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

package de.vitagroup.num.web.config;

public class Role {
  public static final String SUPER_ADMIN = "hasRole('SUPER_ADMIN')";
  public static final String ORGANIZATION_ADMIN = "hasRole('ORGANIZATION_ADMIN')";
  public static final String STUDY_COORDINATOR = "hasRole('STUDY_COORDINATOR')";
  public static final String STUDY_APPROVER = "hasRole('STUDY_APPROVER')";
  public static final String RESEARCHER = "hasRole('RESEARCHER')";
  public static final String CONTENT_ADMIN = "hasRole('CONTENT_ADMIN')";
  public static final String STUDY_COORDINATOR_OR_RESEARCHER =
      "hasAnyRole('STUDY_COORDINATOR', 'RESEARCHER')";
  public static final String STUDY_COORDINATOR_OR_APPROVER =
      "hasAnyRole('STUDY_COORDINATOR', 'STUDY_APPROVER')";
  public static final String STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER =
      "hasAnyRole('STUDY_COORDINATOR', 'RESEARCHER', 'STUDY_APPROVER')";
  public static final String STUDY_COORDINATOR_OR_SUPER_ADMIN =
      "hasAnyRole('SUPER_ADMIN', 'STUDY_COORDINATOR')";
  public static final String SUPER_ADMIN_OR_ORGANIZATION_ADMIN =
      "hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN')";
  public static final String SUPER_ADMIN_OR_ORGANIZATION_ADMIN_OR_STUDY_COORDINATOR =
      "hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'STUDY_COORDINATOR')";
  public static final String ANY_ROLE =
      "hasAnyRole('SUPER_ADMIN', 'ORGANIZATION_ADMIN', 'STUDY_COORDINATOR', 'STUDY_APPROVER', 'RESEARCHER')";

  private Role() {}
}
