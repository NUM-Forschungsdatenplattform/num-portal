package de.vitagroup.num.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    private String description;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cohort_id", referencedColumnName = "id")
    private Cohort cohort;
}
