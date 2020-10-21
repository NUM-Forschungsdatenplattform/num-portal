package de.vitagroup.num.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Expression;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.AttributeConverter;

@Slf4j
public class ExpressionConverter implements AttributeConverter<Expression, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Expression expression) {
        try {
            return mapper.writeValueAsString(expression);
        } catch (JsonProcessingException e) {
            log.error("Cannot convert expression to JSON", e);
        }

        return StringUtils.EMPTY;
    }

    @Override
    public Expression convertToEntityAttribute(String json) {
        try {
            return mapper.readValue(json, Expression.class);
        } catch (JsonProcessingException e) {
            log.error("Cannot convert JSON to expression", e);
        }

        return null;
    }
}
