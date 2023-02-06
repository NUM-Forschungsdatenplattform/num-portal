package de.vitagroup.num.domain;

import de.vitagroup.num.domain.dto.Language;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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