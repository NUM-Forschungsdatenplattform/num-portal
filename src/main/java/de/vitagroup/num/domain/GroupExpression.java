package de.vitagroup.num.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupExpression extends Expression {

    private Operator operator;
    private List<Expression> children;
}
