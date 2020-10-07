package de.vitagroup.num.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Expression;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.AttributeConverter;

@Slf4j
public class ExpressionConverter implements AttributeConverter<Expression, String> {

    @Override
    public String convertToDatabaseColumn(Expression expression) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(expression);
        } catch (JsonProcessingException e) {
            log.error("JSON writing error", e);
        }
        return StringUtils.EMPTY;
    }

    @Override
    public Expression convertToEntityAttribute(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Expression.class);
        } catch (JsonProcessingException e) {
            log.error("JSON reading error", e);
        }
        return null;
    }
}
