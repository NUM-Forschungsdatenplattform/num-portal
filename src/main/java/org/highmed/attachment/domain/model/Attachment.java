package org.highmed.attachment.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Attachment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Column(name = "upload_date", nullable = false)
    private OffsetDateTime uploadDate;

    private String type;

    private byte[] content;

    @Column(name = "author_id")
    private String authorId;

    @Column(name = "review_counter", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int reviewCounter;

    @Column(name = "project_id", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long projectId;


    public Attachment(Long id, String name, String description, OffsetDateTime uploadDate, int reviewCounter) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.uploadDate = uploadDate;
        this.reviewCounter = reviewCounter;
    }
}
