package de.vitagroup.num.attachment.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    private String name;

    private String description;

    private String type;

    private byte[] content;

    private Long projectId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private OffsetDateTime uploadDate;

    private String authorId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private int reviewCounter;
}
