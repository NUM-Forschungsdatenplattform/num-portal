package de.vitagroup.num.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupExpression extends Expression {

    @NotNull(message = "Operator mandatory")
    private Operator operator;

    private List<Expression> children;
}
