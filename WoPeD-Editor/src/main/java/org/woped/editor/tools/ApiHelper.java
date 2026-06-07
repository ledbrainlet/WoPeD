package org.woped.editor.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.woped.core.config.ConfigurationManager;
import org.woped.core.utilities.LoggerManager;
import org.woped.editor.Constants;

public class ApiHelper {

    public static List<String> fetchModels(String apiKey, String provider) throws IOException, ParseException {
        LoggerManager.info(Constants.EDITOR_LOGGER, "Started Fetching GPT Models");

        String baseUrl =
                "http://"
                        + ConfigurationManager.getConfiguration().getProcess2TextServerHost()
                        + ":"
                        + ConfigurationManager.getConfiguration().getProcess2TextServerPort()
                        + ConfigurationManager.getConfiguration().getProcess2TextServerURI()
                        + "/gptModels";
        String encodedApiKey = URLEncoder.encode(apiKey != null ? apiKey : "", StandardCharsets.UTF_8);
        String encodedProvider =
                URLEncoder.encode(provider != null ? provider : "openAi", StandardCharsets.UTF_8);
        String urlString = baseUrl + "?apiKey=" + encodedApiKey + "&provider=" + encodedProvider;
        List<String> models = new ArrayList<>();

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            JSONParser parser = new JSONParser();
            JSONArray modelsArray = (JSONArray) parser.parse(content.toString());
            for (Object model : modelsArray) {
                models.add(model.toString());
            }

            LoggerManager.info(Constants.EDITOR_LOGGER, "Finished Fetching GPT Models");
        } else {
            LoggerManager.error(Constants.EDITOR_LOGGER, "Failed to Fetch GPT Models");
            throw new IOException("Failed to fetch models. Response Code: " + responseCode + "\n"
                    + "Please check your API Key");
        }
        return models;
    }
}
