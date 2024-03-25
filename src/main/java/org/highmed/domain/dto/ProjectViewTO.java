package org.highmed.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.highmed.domain.model.ProjectStatus;
import org.highmed.domain.model.admin.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectViewTO {

    private Long id;

    private String name;

    private User coordinator;

    private ProjectStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    private OffsetDateTime createDate;

}
