package de.vitagroup.num.domain;

import java.util.List;

public enum StudyStatus {
  /** Study creation in progress */
  DRAFT {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(DRAFT, PENDING);
    }
  },

  /** Study is finalized and send for approval but still editable */
  PENDING {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(PENDING, REVIEWING, DRAFT);
    }
  },

  /** Study is being review and cannot be edited */
  REVIEWING {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(REVIEWING, APPROVED, CHANGE_REQUEST, DENIED);
    }
  },

  /** Pending requests from the reviewer */
  CHANGE_REQUEST {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(CHANGE_REQUEST, DRAFT, PENDING);
    }
  },

  /** Study is denied */
  DENIED {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(DENIED);
    }
  },

  /** Study is approved */
  APPROVED {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(APPROVED, PUBLISHED);
    }
  },

  /** Study is published and cannot be edited anymore */
  PUBLISHED {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(PUBLISHED, CLOSED);
    }
  },

  /** Study is finished */
  CLOSED {
    @Override
    public List<StudyStatus> nextStates() {
      return List.of(CLOSED);
    }
  };

  public abstract List<StudyStatus> nextStates();
}
