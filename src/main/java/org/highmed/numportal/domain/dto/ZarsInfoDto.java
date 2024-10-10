package org.highmed.numportal.domain.dto;

import org.highmed.numportal.domain.model.ProjectCategories;
import org.highmed.numportal.domain.model.ProjectStatus;

import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class ZarsInfoDto {

  private int id;
  private String name;
  private ProjectStatus status;
  private String coordinator;
  private LocalDate startDate;
  private LocalDate endDate;
  private String goal;
  private String description;
  private String simpleDescription;
  private Set<String> keywords;
  private Set<ProjectCategories> categories;
  private String queries;
  private String approvalDate;
  private String partners;
  private boolean financed;
  private String closedDate;
}
