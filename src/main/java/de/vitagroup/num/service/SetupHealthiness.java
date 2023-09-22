package de.vitagroup.num.service;

import de.vitagroup.num.domain.model.SetupType;
import de.vitagroup.num.service.html.HtmlContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.EXCEPTION_HAPPENED_IN_CLASS_FOR_ENVIRONMENT;

@Slf4j
@Service
public class SetupHealthiness {

    public Map<String, String> checkHealth(SetupType setup){
        Map<String, String> map = new HashMap<>();
        switch (setup) {
            case PREPROD:
                for (SetupType.Preprod setupType:SetupType.Preprod.values()){
                    map.put(setupType.name(),
                            checkUrl(SetupType.Preprod.class.getSimpleName(), setupType.name(), setupType.getURL()));
                }
                break;
            case PROD:
                for (SetupType.Prod setupType:SetupType.Prod.values()){
                    map.put(setupType.name(),
                            checkUrl(SetupType.Prod.class.getSimpleName(), setupType.name(), setupType.getURL()));
                }
                break;
            case DEV:
                for (SetupType.Dev setupType:SetupType.Dev.values()){
                    map.put(setupType.name(),
                            checkUrl(SetupType.Dev.class.getSimpleName(), setupType.name(), setupType.getURL()));
                }
                break;
            case STAGING:
                for (SetupType.Staging setupType:SetupType.Staging.values()){
                    map.put(setupType.name(),
                            checkUrl(SetupType.Staging.class.getSimpleName(), setupType.name(), setupType.getURL()));
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + setup);
        }
        map.put("CHECK_FOR_ANNOUNCEMENTS", checkForAnnouncements());
        return map;
    }

    private String checkForAnnouncements() {
        String message = Strings.EMPTY;
        String URL = "https://health.num-codex.de/";
        HtmlContent htmlContent = new HtmlContent();
        try {
            htmlContent.init();
            String pageContent = htmlContent.pageContent(URL);
            if(!pageContent.contains( "No current announcements" )){
                message = String.format("Check the %s page for the new announcements", URL);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    private String checkUrl(String preprodClass, String environment, String setupTypeURL) {
        String error;
        try{
            URL url = new URL(setupTypeURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            if(con.getResponseCode() == 200) {
                return Strings.EMPTY;
            } else {
                return String.format(EXCEPTION_HAPPENED_IN_CLASS_FOR_ENVIRONMENT,
                        preprodClass, environment, setupTypeURL);
            }
        }catch (IOException io) {
            error = String.format(EXCEPTION_HAPPENED_IN_CLASS_FOR_ENVIRONMENT,
                    preprodClass, environment, setupTypeURL) + " " + io.getMessage();
            log.error(error);
        }
        return error;
    }

}
