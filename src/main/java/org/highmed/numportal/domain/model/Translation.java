package org.highmed.numportal.domain.model;

import org.highmed.numportal.domain.dto.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Translation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_group", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityGroup entityGroup;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false)
    private String property;

    @Column(nullable = false)
    private String value;

    @Column(name = "language_code", nullable = false)
    @Enumerated(EnumType.STRING)
    private Language language;
}
