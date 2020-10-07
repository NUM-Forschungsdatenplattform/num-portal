package de.vitagroup.num.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AqlExpression extends Expression {

    private Aql aql;
}
