package de.vitagroup.num.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AqlExpression.class, name = "AQL"),
        @JsonSubTypes.Type(value = GroupExpression.class, name = "GROUP")
})
public class Expression implements Serializable {

}
