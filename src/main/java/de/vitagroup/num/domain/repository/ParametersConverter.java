package de.vitagroup.num.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class ParametersConverter implements AttributeConverter<Map<String, String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> parameters) {

        String parametersJson = null;
        try {
            parametersJson = objectMapper.writeValueAsString(parameters);
        } catch (final JsonProcessingException e) {
            log.error("Cannot convert parameters map to JSON", e);
        }

        return parametersJson;
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String parametersJson) {

        Map<String, String> parameters = null;
        try {
            parameters = objectMapper.readValue(parametersJson, Map.class);
        } catch (final IOException e) {
            log.error("Cannot convert parameters JSON to map", e);
        }

        return parameters;
    }
}
