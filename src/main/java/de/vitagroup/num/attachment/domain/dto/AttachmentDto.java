package de.vitagroup.num.attachment.domain.dto;

import lombok.Builder;

@Builder
public class AttachmentDto {

    private String name;

    private String description;

    private String type;

    private byte[] content;
}
