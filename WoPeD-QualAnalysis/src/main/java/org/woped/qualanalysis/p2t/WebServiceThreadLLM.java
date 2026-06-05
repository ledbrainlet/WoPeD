package org.woped.qualanalysis.p2t;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JOptionPane;

import org.woped.core.config.ConfigurationManager;
import org.woped.core.controller.IEditor;
import org.woped.gui.translations.Messages;

public class WebServiceThreadLLM extends Thread {

    private P2TSideBar paraphrasingPanel;
    private boolean isFinished;
    private String apiKey;
    private String prompt;
    private String gptModel;
    private String provider;
    private String useRag;
    private String text;

    public WebServiceThreadLLM(P2TSideBar paraphrasingPanel) {
        this.paraphrasingPanel = paraphrasingPanel;
        isFinished = false;
    }

    public boolean getIsFinished() {
        return isFinished;
    }

    public void run() {
        apiKey = ConfigurationManager.getConfiguration().getGptApiKey();
        prompt = ConfigurationManager.getConfiguration().getGptPrompt();
        gptModel = ConfigurationManager.getConfiguration().getGptModel();
        provider = ConfigurationManager.getConfiguration().getLlmProvider();
        // WFC-US15 (#24): RAG is disabled; always send false regardless of stored config.
        useRag = "false";

        // LoggerManager.info(Constants.EDITOR_LOGGER,"Started Fetching GPT Models");

        IEditor editor = paraphrasingPanel.getEditor();
        paraphrasingPanel.showLoadingAnimation(true);

        // Build URL from configured T2P-LLM service settings (host/port/URI).
        // WFC-US8 (#11): previously hard-coded http://localhost:8080/p2t/generateTextLLM,
        // which ignored the LLM service fields the user sees in the configuration dialog.
        String rawHost = ConfigurationManager.getConfiguration().getT2PLlmServiceHost();
        int port       = ConfigurationManager.getConfiguration().getT2PLlmServicePort();
        String rawUri  = ConfigurationManager.getConfiguration().getT2PLlmServiceUri();

        if (rawHost == null) rawHost = "";
        if (rawUri  == null) rawUri  = "";
        rawHost = rawHost.trim();
        rawUri  = rawUri.trim();

        // Fallback: keep legacy localhost endpoint reachable for dev when no host is configured.
        if (rawHost.isEmpty()) {
            rawHost = "localhost";
            if (port <= 0) port = 8080;
            if (rawUri.isEmpty()) rawUri = "/p2t";
        }

        // Respect explicit scheme in the host field; otherwise pick https for port 443, http for the rest.
        boolean hostHasScheme = rawHost.startsWith("http://") || rawHost.startsWith("https://");
        String scheme   = hostHasScheme ? "" : ((port == 443) ? "https://" : "http://");
        String portPart = (port > 0) ? ":" + port : "";

        // Normalize URI: ensure leading slash, strip trailing slash before appending endpoint.
        String normalizedUri = rawUri.isEmpty()
                ? ""
                : (rawUri.startsWith("/") ? rawUri : "/" + rawUri);
        if (normalizedUri.endsWith("/")) {
            normalizedUri = normalizedUri.substring(0, normalizedUri.length() - 1);
        }

        String url = scheme + rawHost + portPart + normalizedUri + "/generateTextLLM";

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new PNMLExport().saveToStream(editor, stream);
        String text = stream.toString();
        String output;

        try {
            // Guard against unset configuration values before URL-encoding (WFC-US8 #11):
            // previously a missing GPT model selection caused NullPointerException in URLEncoder.
            if (apiKey   == null) apiKey   = "";
            if (prompt   == null) prompt   = "";
            if (gptModel == null) gptModel = "";
            if (provider == null) provider = "";
            if (useRag   == null) useRag   = "false";

            // Encode URL parameters
            String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            String encodedGptModel = URLEncoder.encode(gptModel, StandardCharsets.UTF_8);
            String encodedProvider = URLEncoder.encode(provider, StandardCharsets.UTF_8);
            String encodeduseRag = URLEncoder.encode(useRag, StandardCharsets.UTF_8);
            // Construct URL with parameters
            String urlWithParams = String.format("%s?apiKey=%s&prompt=%s&gptModel=%s&provider=%s&useRag=%s",
                    url, encodedApiKey, encodedPrompt, encodedGptModel, encodedProvider, encodeduseRag);
            URL urlObj = new URL(urlWithParams);

            // Establish connection
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/plain");

            // Send request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = text.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    output = scanner.useDelimiter("\\A").next();
                    output = output.replaceAll("\\s*\n\\s*", "");
                    paraphrasingPanel.setNaturalTextParser(new Process2Text(output));
                    setText(output);
                }
            } else {
                output = "Request failed. Response Code: " + responseCode;
            }

            // Fehlerbehandlung basierend auf Response Code

            switch (responseCode) {
                case HttpServletResponse.SC_NO_CONTENT:
                case HttpServletResponse.SC_REQUEST_TIMEOUT:
                case HttpServletResponse.SC_INTERNAL_SERVER_ERROR:
                    JOptionPane.showMessageDialog(
                            null,
                            Messages.getString("Paraphrasing.Webservice.Error.TryAgain"),
                            Messages.getString("Paraphrasing.Webservice.Error.Title"),
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                case HttpServletResponse.SC_SERVICE_UNAVAILABLE:
                case HttpServletResponse.SC_NOT_FOUND:
                case HttpServletResponse.SC_METHOD_NOT_ALLOWED:
                case -1:
                    JOptionPane.showMessageDialog(
                            null,
                            Messages.getString("Paraphrasing.Webservice.Error.Contact")
                                    + "\n"
                                    + Messages.getString("Paraphrasing.Webservice.Settings"),
                            Messages.getString("Paraphrasing.Webservice.Error.Title"),
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error processing request: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            isFinished = true;
            paraphrasingPanel.showLoadingAnimation(false);
            paraphrasingPanel.enableButtons(true);
            paraphrasingPanel.setThreadInProgress(false);
        }
    }

    // Setter- und Getter für das Ergebnis des LLM

    public void setText(String output) {
        this.text = output;
    }

    public String getText() {
        return this.text;
    }
}
