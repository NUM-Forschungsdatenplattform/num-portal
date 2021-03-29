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

import static de.vitagroup.num.domain.Roles.STUDY_APPROVER;
import static de.vitagroup.num.domain.Roles.STUDY_COORDINATOR;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum StudyStatus {
  /** Study creation in progress */
  DRAFT {
    @Override
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          DRAFT, Arrays.asList(STUDY_COORDINATOR),
          PENDING, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Study is finalized and send for approval but still editable */
  PENDING {
    @Override
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          PENDING, Arrays.asList(STUDY_COORDINATOR),
          REVIEWING, Arrays.asList(STUDY_APPROVER),
          DRAFT, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Study is being review and cannot be edited */
  REVIEWING {
    @Override
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
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
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          CHANGE_REQUEST, Arrays.asList(STUDY_COORDINATOR),
          DRAFT, Arrays.asList(STUDY_COORDINATOR),
          PENDING, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Study is denied */
  DENIED {
    @Override
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(DENIED, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Study is approved */
  APPROVED {
    @Override
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          APPROVED, Arrays.asList(STUDY_COORDINATOR),
          PUBLISHED, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Study is published and cannot be edited anymore */
  PUBLISHED {
    @Override
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(
          PUBLISHED, Arrays.asList(STUDY_COORDINATOR),
          CLOSED, Arrays.asList(STUDY_COORDINATOR));
    }
  },

  /** Study is finished */
  CLOSED {
    @Override
    public Map<StudyStatus, List<String>> nextStatusesAndRoles() {
      return Map.of(CLOSED, Arrays.asList(STUDY_COORDINATOR));
    }
  };

  public abstract Map<StudyStatus, List<String>> nextStatusesAndRoles();
}
