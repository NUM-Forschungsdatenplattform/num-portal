package de.vitagroup.num.domain.dto;

import de.vitagroup.num.domain.StudyCategories;
import de.vitagroup.num.domain.StudyStatus;
import java.time.LocalDate;
import java.util.Set;
import lombok.Data;

@Data
public class ZarsInfoDto {
  private int id;
  private String name;
  private StudyStatus status;
  private String coordinator;
  private LocalDate startDate;
  private LocalDate endDate;
  private String goal;
  private String description;
  private String simpleDescription;
  private Set<String> keywords;
  private Set<StudyCategories> categories;
  private String queries;
  private String approvalDate;
  private String partners;
  private boolean financed;
  private String closedDate;
}