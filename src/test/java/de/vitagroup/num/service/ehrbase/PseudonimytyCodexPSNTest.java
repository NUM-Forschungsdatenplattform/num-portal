package de.vitagroup.num.service.ehrbase;

import de.vitagroup.num.config.FttpClientConfig;
import de.vitagroup.num.properties.FttpProperties;
import de.vitagroup.num.properties.PrivacyProperties;
import de.vitagroup.num.properties.PseudonymsPsnWorkflowProperties;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Pseudonymity.class, FttpClientConfig.class})
@EnableConfigurationProperties(value = {FttpProperties.class, PseudonymsPsnWorkflowProperties.class, PrivacyProperties.class})
@ActiveProfiles("pseudo")
public class PseudonimytyCodexPSNTest {

    @Autowired
    private Pseudonymity pseudonymity;
    private final static String CSV_FILE_ENDING = ".csv";
    private final static String CSV_COMMA_DELIMITER = ",";

    // comment ignore if a new bunch of codes should be tested
    @Ignore
    @Test
    public void testCodexPsn() throws IOException {
        List<String> secondLevelPseudonyms = new ArrayList<>();
        String header = "original,pseudonym";
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(IOUtils.toBufferedInputStream(getClass()
                             .getResourceAsStream("/codex-test-psns/codex_psns_test_system.csv"))))) {
            String code;
            while ((code = reader.readLine()) != null) {
                secondLevelPseudonyms.add(code);
            }
        }
        Long projectId = 1L;
        FileWriter fileWriter = new FileWriter("src/test/resources/codex-test-psns" + File.separator +
                String.format("codex_result_%d_%s%s", projectId,
                        LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        CSV_FILE_ENDING));
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(header);
        bufferedWriter.newLine();
        for (String code : secondLevelPseudonyms) {
            List<String> response = pseudonymity.getPseudonyms(List.of(code), projectId);
            String currentLine = code + CSV_COMMA_DELIMITER + response.get(0);
            bufferedWriter.write(currentLine);
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
    }
}
