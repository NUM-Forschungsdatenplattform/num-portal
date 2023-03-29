package de.vitagroup.num.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.vitagroup.num.domain.ProjectStatus;
import de.vitagroup.num.domain.admin.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
