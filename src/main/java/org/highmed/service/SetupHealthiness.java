package org.highmed.service;

import org.highmed.domain.model.SetupType;
import org.highmed.properties.EhrBaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.util.Strings;
import org.highmed.service.html.HtmlContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.highmed.domain.templates.ExceptionsTemplate.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SetupHealthiness {

    private final HtmlContent htmlContent;

    private final EhrBaseProperties ehrBaseProperties;

    @Value("${num.systemStatusUrl}")
    private String systemStatusUrl;

    @Value("${num.statusCakeUrl}")
    private String statusCakeUrl;

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
        return map;
    }

    public String checkForAnnouncements() {
        String message = Strings.EMPTY;
        try {
            String pageContent = htmlContent.pageContent(systemStatusUrl);
            String publicID = getPublicID(pageContent);
            if(publicID.isEmpty()){
                return message;
            }
            String dynamicContent = getDynamicPageContent(publicID, htmlContent);
            String timeOfAnnouncement = getTimeOfAnnouncement(dynamicContent, true);
            String descriptionOfAnnouncement = getTimeOfAnnouncement(dynamicContent, false);
            if (timeOfAnnouncement.isEmpty() && descriptionOfAnnouncement.isEmpty()){
                return message;
            }
            if (!timeOfAnnouncement.isEmpty() && timeOfAnnouncement.length()<4){
                return message;
            }
            if (!descriptionOfAnnouncement.isEmpty() && descriptionOfAnnouncement.length()<4){
                return message;
            }
            if(!timeOfAnnouncement.isEmpty() && !descriptionOfAnnouncement.isEmpty()){
                return String.format(ANNOUNCEMENT_IN_PLACE, timeOfAnnouncement, descriptionOfAnnouncement);
            }
            if(!dynamicContent.contains( "No current announcements" )){
                message = String.format("Check the %s page for the new announcements", systemStatusUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return message;
    }

    private String getTimeOfAnnouncement(String dynamicContent, boolean isTimeOfAnnouncement) {
        if(isTimeOfAnnouncement){
            Pattern onlyImageLink = Pattern.compile("<td style=\\\\\"width:50%;\\\\\">([0-9,:,\\\\,\\/]*)\\w+");
            Matcher m = onlyImageLink.matcher( dynamicContent );
            if (m.find()) {
               return m.group( 0 ).replace("<td style=\\\"width:50%;\\\">","");
            }
        } else {
            Pattern fullMatching = Pattern.compile("<td style=\\\\\"width:50%;\\\\\">([A-Z]!*)\\w+");
            Matcher m = fullMatching.matcher(dynamicContent);
            if (m.find()) {
                return m.group(0).replace("<td style=\\\"width:50%;\\\">", "");
            }
        }
        return Strings.EMPTY;
    }

    private String getDynamicPageContent(String publicID, HtmlContent htmlContent) throws IOException, URISyntaxException {
        String newURL = statusCakeUrl + publicID;
        return htmlContent.pageContent(newURL);
    }

    private String getPublicID(String pageContent) {
        try {
            String[] arr = new String[0];
            if (pageContent.contains("var PublicID")) {
                int i = pageContent.lastIndexOf("var PublicID");
                String substring = pageContent.substring(i, i + 37);
                arr = substring.split("'");
                if (arr.length < 2) {
                    return Strings.EMPTY;
                }
            }
            return arr[1];
        } catch (IndexOutOfBoundsException index){
            throw new IndexOutOfBoundsException(EXCEPTION_IN_PARSING_PAGE + " " + index.getMessage());
        }
    }

    private String checkUrl(String preprodClass, String environment, String setupTypeURL) {
        String error;
        try{
            URL url = new URL(setupTypeURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Authorization", getBasicAuthenticationHeader(ehrBaseProperties.getAdminUsername(),
                    ehrBaseProperties.getAdminPassword()));
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

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.encodeBase64String(valueToEncode.getBytes(StandardCharsets.UTF_8));
    }

}
