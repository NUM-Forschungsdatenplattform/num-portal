package de.vitagroup.num.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.vitagroup.num.domain.Aql;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class AqlService {

    private final ObjectMapper mapper;

    public List<Aql> getAllAqls() {
        return getMockedAqls();
    }

    private List<Aql> getMockedAqls() {
        File firstFile = new File(getClass().getClassLoader().getResource("mock/aql/aql_1.json").getFile());
        File secondFile = new File(getClass().getClassLoader().getResource("mock/aql/aql_2.json").getFile());

        try {
            String first = FileUtils.readFileToString(firstFile, StandardCharsets.UTF_8);
            String second = FileUtils.readFileToString(secondFile, StandardCharsets.UTF_8);
            Aql secondAql = mapper.readValue(second, Aql.class);
            Aql firstAql = mapper.readValue(first, Aql.class);
            return Arrays.asList(firstAql, secondAql);
        } catch (IOException e) {
           log.error("Error reading mock aqls from file");
        }

        return Collections.emptyList();
    }
}
