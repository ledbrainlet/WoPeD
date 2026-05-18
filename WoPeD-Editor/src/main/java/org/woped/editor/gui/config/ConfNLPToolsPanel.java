package org.woped.editor.gui.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.json.simple.parser.ParseException;
import org.woped.core.config.ConfigurationManager;
import org.woped.editor.tools.ApiHelper;
import org.woped.gui.lookAndFeel.WopedButton;
import org.woped.gui.translations.Messages;

public class ConfNLPToolsPanel extends AbstractConfPanel {
    private static final int SETTINGS_LABEL_WIDTH = 155;
    private static final int SETTINGS_LABEL_RIGHT_PADDING = 10;

    // Enable automatic intermediate certificate fetching
    static {
        // Enable AIA (Authority Information Access) certificate fetching
        // This allows Java to download missing intermediate certificates automatically
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
        // Also enable CRL checking if needed
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        
        // Load custom truststore as fallback
        loadCustomTruststore();
    }
    
    private JCheckBox useBox = null;
    private JPanel enabledPanel = null;
    private JPanel settingsPanel = null;
    private JPanel settingsPanel_T2P = null;
    private JPanel settingsPanel_GPT = null;

    private JTextField serverURLText = null;
    private JLabel serverURLLabel = null;
    private JLabel serverPortLabel = null;
    private JTextField serverPortText = null;
    private JTextField managerPathText = null;
    private JLabel managerPathLabel = null;
    private WopedButton testButton = null;
    private WopedButton defaultButton = null;
    private JTextField serverURLText_T2P = null;
    private JLabel serverURLLabel_T2P = null;
    private JLabel serverPortLabel_T2P = null;
    private JTextField serverPortText_T2P = null;
    private JTextField managerPathText_T2P = null;
    private JLabel managerPathLabel_T2P = null;
    private WopedButton testButton_T2P = null;
    private WopedButton defaultButton_T2P = null;

    // Components for additionalPanel
    private JTextField apiKeyText = null;
    private JCheckBox showAgainBox = null;
    private JCheckBox ragOptionBox = null;
    private WopedButton resetButton = null;
    private JTextArea promptText = null;
    private JTextArea promptTextT2P = null;
    private WopedButton fetchGPTModelsButton = null;
    private WopedButton checkConnectionButton = null;
    private JComboBox<String> modelComboBox = new JComboBox<String>();

    // Components for LLM Panel
    private JPanel settingsPanel_LLM = null;
    private JTextField serviceUrlText_LLM = null;
    private JLabel serviceUrlLabel_LLM = null;
    private JTextField servicePortText_LLM = null;
    private JLabel servicePortLabel_LLM = null;
    private JTextField serviceUriText_LLM = null;
    private JLabel serviceUriLabel_LLM = null;
    private WopedButton testButton_LLM = null;
    private WopedButton defaultButton_LLM = null;
    // Neue Felder hinzufügen (nach den anderen privaten Feldern):
    private JLabel providerLabel = null;
    private JComboBox<String> providerComboBox = null;

    // WFC-US5 (#6): suppresses the provider-change auto-fetch while readConfiguration()
    // is running, so opening the dialog does not trigger a fetch (and a 401 dialog)
    // against a possibly stale stored API key.
    private boolean readingConfig = false;

    // WFC-US12 (#17): inline API-key validator state
    private JLabel apiKeyStatusLabel = null;
    private JPanel apiKeyContainer  = null;

    public ConfNLPToolsPanel(String name) {
        super(name);
        initialize();
    }

    public boolean applyConfiguration() {
        boolean newsetting = useBox.isSelected();
        boolean oldsetting = ConfigurationManager.getConfiguration().getProcess2TextUse();

        if (newsetting != oldsetting) {
            ConfigurationManager.getConfiguration().setProcess2TextUse(newsetting);
            JOptionPane.showMessageDialog(
                    this,
                    Messages.getString("Configuration.P2T.Dialog.Restart.Message"),
                    Messages.getString("Configuration.P2T.Dialog.Restart.Title"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        ConfigurationManager.getConfiguration().setProcess2TextServerHost(getServerURLText().getText());
        ConfigurationManager.getConfiguration().setProcess2TextServerURI(getManagerPathText().getText());
        if (getServerPortText().getText().isEmpty()) {
            ConfigurationManager.getConfiguration().setProcess2TextServerPort(0);
        } else {
            ConfigurationManager.getConfiguration()
                    .setProcess2TextServerPort(Integer.parseInt(getServerPortText().getText()));
        }
        ConfigurationManager.getConfiguration().setProcess2TextUse(useBox.isSelected());

        ConfigurationManager.getConfiguration().setText2ProcessServerHost(getServerURLText_T2P().getText());
        ConfigurationManager.getConfiguration().setText2ProcessServerURI(getManagerPathText_T2P().getText());

        if (getServerPortText_T2P().getText().isEmpty()) {
            ConfigurationManager.getConfiguration().setText2ProcessServerPort(0);
        } else {
            ConfigurationManager.getConfiguration()
                    .setText2ProcessServerPort(Integer.parseInt(getServerPortText_T2P().getText()));
        }

        // Provider-Konfiguration speichern
        ConfigurationManager.getConfiguration().setLlmProvider((String) getProviderComboBox().getSelectedItem());
        ConfigurationManager.getConfiguration().setGptApiKey(getApiKeyText().getText());
        ConfigurationManager.getConfiguration().setGptShowAgain(getShowAgainBox().isSelected());
        ConfigurationManager.getConfiguration().setGptPrompt(getPromptText().getText());
        ConfigurationManager.getConfiguration().setGptPromptT2P(getPromptTextT2P().getText());
        ConfigurationManager.getConfiguration().setT2PLlmServiceHost(getServiceUrlText_LLM().getText());
        if (getServicePortText_LLM().getText().isEmpty()) {
            ConfigurationManager.getConfiguration().setT2PLlmServicePort(0);
        } else {
            ConfigurationManager.getConfiguration()
                    .setT2PLlmServicePort(Integer.parseInt(getServicePortText_LLM().getText()));
        }
        ConfigurationManager.getConfiguration().setT2PLlmServiceUri(getServiceUriText_LLM().getText());
        ConfigurationManager.getConfiguration().setRagOption(getRagOptionBox().isSelected());
        if (modelComboBox.getSelectedItem() != null) {
            ConfigurationManager.getConfiguration().setGptModel(modelComboBox.getSelectedItem().toString());
        }

        return true;
    }

    public void readConfiguration() {
        readingConfig = true;
        try {
            getServerURLText().setText(ConfigurationManager.getConfiguration().getProcess2TextServerHost());
            getManagerPathText().setText(ConfigurationManager.getConfiguration().getProcess2TextServerURI());
            getServerPortText().setText("" + ConfigurationManager.getConfiguration().getProcess2TextServerPort());
            getUseBox().setSelected(ConfigurationManager.getConfiguration().getProcess2TextUse());

            getServerURLText_T2P().setText(ConfigurationManager.getConfiguration().getText2ProcessServerHost());
            getManagerPathText_T2P().setText(ConfigurationManager.getConfiguration().getText2ProcessServerURI());
            getServerPortText_T2P().setText("" + ConfigurationManager.getConfiguration().getText2ProcessServerPort());

            // Provider-Konfiguration laden
            String provider = ConfigurationManager.getConfiguration().getLlmProvider();
            if (provider != null && !provider.isEmpty()) {
                getProviderComboBox().setSelectedItem(provider);
            } else {
                getProviderComboBox().setSelectedItem("openAi"); // Default
            }

            getApiKeyText().setText(ConfigurationManager.getConfiguration().getGptApiKey());
            getShowAgainBox().setSelected(ConfigurationManager.getConfiguration().getGptShowAgain());
            getPromptText().setText(ConfigurationManager.getConfiguration().getGptPrompt());
            getPromptTextT2P().setText(ConfigurationManager.getConfiguration().getGptPromptT2P());
            getServiceUrlText_LLM().setText(ConfigurationManager.getConfiguration().getT2PLlmServiceHost());
            getServicePortText_LLM().setText("" + ConfigurationManager.getConfiguration().getT2PLlmServicePort());
            getServiceUriText_LLM().setText(ConfigurationManager.getConfiguration().getT2PLlmServiceUri());
            getRagOptionBox().setSelected(ConfigurationManager.getConfiguration().getRagOption());
        } finally {
            readingConfig = false;
        }

        // WFC-US6 (#7): once the dialog values are loaded, kick off a silent API-key
        // validation and model fetch on the EDT so the user sees the saved key's
        // verdict and the saved model in the dropdown without manual clicks.
        SwingUtilities.invokeLater(this::triggerInitialChecks);
    }

    private void initialize() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        contentPanel.add(getEnabledPanel(), c);

        // WFC-US21 (#26): the P2T server settings panel was hidden in WFC-US1 and
        // re-added here after Prof. Freytag asked for the panel back (feedback
        // on 2026-05-11, communicated via Eduardo).
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 1;
        contentPanel.add(getSettingsPanel(), c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 2;
        contentPanel.add(getSettingsPanel_T2P(), c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 3;
        contentPanel.add(getSettingsPanel_LLM(), c);

        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 4;
        contentPanel.add(getGPTPanel(), c);

        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        c.gridy = 5;
        contentPanel.add(new JPanel(), c);

        setMainPanel(contentPanel);

    }

    private JTextField getServerURLText() {
        if (serverURLText == null) {
            serverURLText = new JTextField();
            serverURLText.setColumns(40);
            serverURLText.setEnabled(true);
            serverURLText
                    .setToolTipText("<html>" + Messages.getString("Configuration.P2T.Label.ServerHost") + "</html>");
        }
        return serverURLText;
    }

    private JTextField getServerURLText_T2P() {
        if (serverURLText_T2P == null) {
            serverURLText_T2P = new JTextField();
            serverURLText_T2P.setColumns(40);
            serverURLText_T2P.setEnabled(true);
            serverURLText_T2P
                    .setToolTipText("<html>" + Messages.getString("Configuration.T2P.Label.ServerHost") + "</html>");
        }
        return serverURLText_T2P;
    }

    private JPanel getEnabledPanel() {
        if (enabledPanel == null) {
            enabledPanel = new JPanel();
            enabledPanel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(2, 0, 2, 0);

            enabledPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(Messages.getTitle("Configuration.P2T.Enabled.Panel")),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            c.weightx = 1;
            c.gridx = 0; // Move further left
            c.gridy = 0;
            enabledPanel.add(getUseBox(), c);
        }
        return enabledPanel;
    }

    private JLabel alignSettingsLabel(JLabel label) {
        Dimension preferredSize = label.getPreferredSize();
        Dimension fixedSize = new Dimension(SETTINGS_LABEL_WIDTH, preferredSize.height);
        label.setPreferredSize(fixedSize);
        label.setMinimumSize(fixedSize);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, SETTINGS_LABEL_RIGHT_PADDING));
        return label;
    }

    private JLabel createSettingsLabel(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(JLabel.RIGHT);
        return alignSettingsLabel(label);
    }

    private JPanel getSettingsPanel() {
        if (settingsPanel == null) {
            settingsPanel = new JPanel();
            settingsPanel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(2, 0, 2, 0);

            settingsPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(Messages.getString("Configuration.P2T.Settings.Panel.Title")),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 0;
            settingsPanel.add(alignSettingsLabel(getServerURLLabel()), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel.add(getServerURLText(), c);
            c.fill = GridBagConstraints.NONE;

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            settingsPanel.add(alignSettingsLabel(getServerPortLabel()), c);

            c.weightx = 0;
            c.gridx = 1;
            c.gridy = 1;
            settingsPanel.add(getServerPortText(), c);

            c.weightx = 0;
            c.gridx = 2;
            c.gridy = 1;
            settingsPanel.add(getTestButton(), c);

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 2;
            settingsPanel.add(alignSettingsLabel(getManagerPathLabel()), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel.add(getManagerPathText(), c);
            c.fill = GridBagConstraints.NONE;

            c.weightx = 0;
            c.gridx = 3;
            c.gridy = 1;
            settingsPanel.add(getDefaultButton(), c);

            // WFC-US22 (#27): P2T-Prompt under the server settings
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 1;
            settingsPanel.add(createSettingsLabel(Messages.getString("Configuration.GPT.prompt.P2T.Title")), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 3;
            c.gridwidth = 3;
            settingsPanel.add(getPromptTextScrollPane(), c);

        }

        settingsPanel.setVisible(getUseBox().isSelected());
        return settingsPanel;
    }

    private JPanel getSettingsPanel_T2P() {
        if (settingsPanel_T2P == null) {
            settingsPanel_T2P = new JPanel();
            settingsPanel_T2P.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(2, 0, 2, 0);

            settingsPanel_T2P.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(Messages.getString("Configuration.T2P.Settings.Panel.Title_NLP")),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 0;
            settingsPanel_T2P.add(alignSettingsLabel(getServerURLLabel_T2P()), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel_T2P.add(getServerURLText_T2P(), c);
            c.fill = GridBagConstraints.NONE;

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            settingsPanel_T2P.add(alignSettingsLabel(getServerPortLabel_T2P()), c);

            c.weightx = 0;
            c.gridx = 1;
            c.gridy = 1;
            settingsPanel_T2P.add(getServerPortText_T2P(), c);

            c.weightx = 0;
            c.gridx = 2;
            c.gridy = 1;
            settingsPanel_T2P.add(getTestButton_T2P(), c);

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 2;
            settingsPanel_T2P.add(alignSettingsLabel(getManagerPathLabel_T2P()), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel_T2P.add(getManagerPathText_T2P(), c);
            c.fill = GridBagConstraints.NONE;

            c.weightx = 0;
            c.gridx = 3;
            c.gridy = 1;
            settingsPanel_T2P.add(getDefaultButton_T2P(), c);

        }

        settingsPanel_T2P.setVisible(getUseBox_T2P().isSelected());
        return settingsPanel_T2P;
    }

    private JPanel getSettingsPanel_LLM() {
        if (settingsPanel_LLM == null) {
            settingsPanel_LLM = new JPanel();
            settingsPanel_LLM.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(2, 0, 2, 0);

            settingsPanel_LLM.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(Messages.getString("Configuration.T2P.Settings.Panel.Title_LLM")),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 0;
            settingsPanel_LLM.add(alignSettingsLabel(getServiceUrlLabel_LLM()), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel_LLM.add(getServiceUrlText_LLM(), c);
            c.fill = GridBagConstraints.NONE;

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            settingsPanel_LLM.add(alignSettingsLabel(getServicePortLabel_LLM()), c);

            c.weightx = 0;
            c.gridx = 1;
            c.gridy = 1;
            settingsPanel_LLM.add(getServicePortText_LLM(), c);

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 1;
            settingsPanel_LLM.add(alignSettingsLabel(getServiceUriLabel_LLM()), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel_LLM.add(getServiceUriText_LLM(), c);
            c.fill = GridBagConstraints.NONE;

            // Test- und Default-Button auf die Port-Zeile, analog NLP-Panel (WFC-US1)
            c.weightx = 0;
            c.gridx = 2;
            c.gridy = 1;
            c.gridwidth = 1;
            settingsPanel_LLM.add(getTestButton_LLM(), c);

            c.weightx = 0;
            c.gridx = 3;
            c.gridy = 1;
            settingsPanel_LLM.add(getDefaultButton_LLM(), c);

            // WFC-US22 (#27): T2P-Prompt under the LLM server settings
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 1;
            settingsPanel_LLM.add(createSettingsLabel(Messages.getString("Configuration.GPT.prompt.T2P.Title")), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 3;
            c.gridwidth = 3;
            settingsPanel_LLM.add(getPromptTextScrollPaneT2P(), c);

        }

        settingsPanel_LLM.setVisible(getUseBox().isSelected());
        return settingsPanel_LLM;
    }

    private JComboBox<String> getProviderComboBox() {
        if (providerComboBox == null) {
            providerComboBox = new JComboBox<>(new String[] { "openAi", "gemini", "lmStudio" });
            providerComboBox.setEnabled(true);
            providerComboBox.setToolTipText("Select LLM Provider");
            providerComboBox.addActionListener(e -> {
                // Nur noch Modelle leeren und API Key Sichtbarkeit ändern
                modelComboBox.removeAllItems();

                // API Key Feld verstecken bei LM Studio
                String selectedProvider = (String) providerComboBox.getSelectedItem();
                boolean showApiKey = !"lmStudio".equals(selectedProvider);

                // API Key Label und Feld ein-/ausblenden
                Component[] components = getGPTPanel().getComponents();
                for (Component comp : components) {
                    if (comp instanceof JLabel) {
                        JLabel label = (JLabel) comp;
                        if (Messages.getString("Configuration.GPT.apikey.Title").equals(label.getText())) {
                            label.setVisible(showApiKey);
                            break;
                        }
                    }
                }
                // WFC-US12 (#17): hide the wrapping container (text field + status label) together.
        getApiKeyAndStatus().setVisible(showApiKey);

                // Panel neu zeichnen
                getGPTPanel().revalidate();
                getGPTPanel().repaint();

                // WFC-US12 (#17): refresh the API-key status indicator for the new provider
                // (format expectations differ between openAi / gemini / lmStudio).
                runApiKeyFormatCheck();

                // WFC-US5 (#6): auto-fetch models for the newly selected provider as soon
                // as we have what we need (lmStudio: nothing, others: a non-empty API key).
                // Skip during readConfiguration() so opening the dialog doesn't fire a
                // fetch against a stale stored key. WFC-US6 (#7): silent on error here —
                // the user just changed providers, an alert popup would be noise.
                if (!readingConfig) {
                    String key = getApiKeyText().getText();
                    boolean canFetch = "lmStudio".equals(selectedProvider)
                            || (key != null && !key.trim().isEmpty());
                    if (canFetch) {
                        fetchAndFillModels(true);
                    }
                }
            });
        }
        return providerComboBox;
    }

    private JLabel getProviderLabel() {
        if (providerLabel == null) {
            providerLabel = new JLabel(Messages.getString("P2T.provider.title"));
            providerLabel.setHorizontalAlignment(JLabel.RIGHT);
        }
        return providerLabel;
    }

    private JPanel getGPTPanel() {
        if (settingsPanel_GPT == null) {
            settingsPanel_GPT = new JPanel();
            settingsPanel_GPT.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(2, 0, 2, 0);

            settingsPanel_GPT.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(Messages.getString("Configuration.GPT.settings.Title")),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            // Provider Selection (Row 0)
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 1;
            settingsPanel_GPT.add(alignSettingsLabel(getProviderLabel()), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel_GPT.add(getProviderComboBox(), c);

            // Model Selection (Row 1) — directly under Provider per Prof. Freytag's
            // request in WFC-US5 (#6).
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            settingsPanel_GPT.add(createSettingsLabel(Messages.getString("Configuration.GPT.model.Title")), c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 1;
            c.insets = new Insets(2, 0, 2, 12);
            c.fill = GridBagConstraints.HORIZONTAL;
            settingsPanel_GPT.add(getModelComboBox(), c);

            // WFC-US23 (#28): the "GPT-Modelle abrufen" button is hidden from
            // the UI per Prof. Freytag's feedback on 2026-05-11. Model fetching
            // now runs automatically when the dialog opens (WFC-US6) and on
            // provider change (WFC-US5). The block below is left commented out
            // so it can be re-enabled cleanly (e.g. as an icon button in
            // WFC-US24).
            // c.weightx = 0;
            // c.gridx = 2;
            // c.gridy = 1;
            // c.fill = GridBagConstraints.NONE;
            // c.insets = new Insets(2, 0, 2, 10);
            // settingsPanel_GPT.add(getFetchGPTModelsButton(), c);

            // API Key (Row 2) — Label als Variable speichern für spätere Referenz
            JLabel apiKeyLabel = createSettingsLabel(Messages.getString("Configuration.GPT.apikey.Title"));
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 2;
            c.insets = new Insets(2, 0, 2, 0);
            c.fill = GridBagConstraints.NONE;
            settingsPanel_GPT.add(apiKeyLabel, c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            settingsPanel_GPT.add(getApiKeyAndStatus(), c);

            // RAG checkbox next to the API key field
            c.weightx = 0;
            c.gridx = 2;
            c.gridy = 2;
            c.gridwidth = 1;
            c.insets = new Insets(2, 10, 2, 10);
            settingsPanel_GPT.add(getRagOptionBox(), c);

            // WFC-US22 (#27): the T2P/P2T prompts moved into their respective
            // server-settings panels (getSettingsPanel for P2T, getSettingsPanel_LLM
            // for T2P LLM). GPT-Einstellungen keeps only provider/key/model.

            // Show Again Checkbox (Row 3)
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 1;
            c.insets = new Insets(2, 0, 2, 0);
            settingsPanel_GPT.add(getShowAgainBox(), c);

            // Check Connection Button (Row 3)
            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 3;
            c.insets = new Insets(2, 0, 2, 12);
            settingsPanel_GPT.add(getCheckConnectionButton(), c);

            // Reset Button (Row 3)
            c.weightx = 1;
            c.gridx = 2;
            c.gridy = 3;
            c.insets = new Insets(2, 0, 2, 10);
            settingsPanel_GPT.add(getResetButton(), c);

        }

        settingsPanel_GPT.setVisible(getUseBox().isSelected());

        // Initial API Key Sichtbarkeit basierend auf aktuellem Provider setzen
        String currentProvider = (String) getProviderComboBox().getSelectedItem();
        boolean showApiKey = !"lmStudio".equals(currentProvider);

        // API Key Komponenten finden und Sichtbarkeit setzen
        Component[] components = settingsPanel_GPT.getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (Messages.getString("Configuration.GPT.apikey.Title").equals(label.getText())) {
                    label.setVisible(showApiKey);
                    break;
                }
            }
        }
        // WFC-US12 (#17): hide the wrapping container (text field + status label) together.
        getApiKeyAndStatus().setVisible(showApiKey);

        // Model selection basierend auf gespeicherter Konfiguration setzen
        for (int i = 0; i < modelComboBox.getItemCount(); i++) {
            if (modelComboBox.getItemAt(i).equals(ConfigurationManager.getConfiguration().getGptModel())) {
                modelComboBox.setSelectedIndex(i);
                break;
            }
        }
        return settingsPanel_GPT;
    }

    private JScrollPane getPromptTextScrollPane() {
        JScrollPane scrollPane = new JScrollPane(getPromptText());
        scrollPane.setPreferredSize(new Dimension(520, 100));
        return scrollPane;
    }

    private JTextField getApiKeyText() {
        if (apiKeyText == null) {
            apiKeyText = new JTextField();
            apiKeyText.setColumns(40);
            apiKeyText.setEnabled(true);
            // WFC-US12 (#17): instant format check on every keystroke,
            // provider-side validation when the user leaves the field.
            apiKeyText.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e)  { runApiKeyFormatCheck(); }
                @Override public void removeUpdate(DocumentEvent e)  { runApiKeyFormatCheck(); }
                @Override public void changedUpdate(DocumentEvent e) { runApiKeyFormatCheck(); }
            });
            apiKeyText.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    runApiKeyApiCheck();
                    // WFC-US22 (#27): entering / changing the API key also
                    // triggers a silent model fetch so the dropdown is
                    // populated when the key changes (e.g. first-time setup).
                    String key = apiKeyText.getText();
                    String provider = (String) getProviderComboBox().getSelectedItem();
                    boolean canFetch = "lmStudio".equals(provider)
                            || (key != null && !key.trim().isEmpty());
                    if (canFetch) {
                        fetchAndFillModels(true);
                    }
                }
            });
        }
        return apiKeyText;
    }

    /**
     * WFC-US12 (#17): small status label next to the API key field.
     * Shows a small status icon plus a one-word state.
     */
    private JLabel getApiKeyStatusLabel() {
        if (apiKeyStatusLabel == null) {
            apiKeyStatusLabel = new JLabel(" ");
            apiKeyStatusLabel.setIconTextGap(4);
            apiKeyStatusLabel.setPreferredSize(new Dimension(120, 20));
        }
        return apiKeyStatusLabel;
    }

    /**
     * WFC-US12 (#17): wraps the API key text field together with the status label
     * so they sit side by side inside the GPT panel grid cell.
     */
    private JPanel getApiKeyAndStatus() {
        if (apiKeyContainer == null) {
            apiKeyContainer = new JPanel();
            apiKeyContainer.setLayout(new BoxLayout(apiKeyContainer, BoxLayout.X_AXIS));
            apiKeyContainer.add(getApiKeyText());
            apiKeyContainer.add(Box.createHorizontalStrut(8));
            apiKeyContainer.add(getApiKeyStatusLabel());
        }
        return apiKeyContainer;
    }

    /**
     * WFC-US12 (#17): synchronous format-only check on the API key. Runs on
     * every keystroke and on provider change. No network call here.
     */
    private void runApiKeyFormatCheck() {
        JLabel status   = getApiKeyStatusLabel();
        String key      = (apiKeyText == null) ? "" : apiKeyText.getText();
        String provider = (String) getProviderComboBox().getSelectedItem();

        // lmStudio needs no key, and an empty key is shown as a neutral state.
        if ("lmStudio".equals(provider) || key == null || key.trim().isEmpty()) {
            status.setText(" ");
            status.setIcon(null);
            status.setForeground(Color.GRAY);
            status.setToolTipText(null);
            return;
        }

        boolean formatOk;
        if ("openAi".equals(provider)) {
            formatOk = key.startsWith("sk-") && key.length() >= 20;
        } else if ("gemini".equals(provider)) {
            formatOk = key.startsWith("AIza") && key.length() >= 30;
        } else {
            formatOk = true;
        }

        if (!formatOk) {
            status.setIcon(Messages.getImageIcon("Configuration.GPT.apikey.status.format.bad"));
            status.setText(Messages.getString("Configuration.GPT.apikey.status.format.bad"));
            status.setForeground(new Color(192, 128, 0));
            status.setToolTipText(Messages.getString("Configuration.GPT.apikey.status.tooltip.format"));
        } else {
            // Format looks fine - leave the verdict to the API check on focus loss.
            status.setText(" ");
            status.setIcon(null);
            status.setForeground(Color.GRAY);
            status.setToolTipText(null);
        }
    }

    /**
     * WFC-US12 (#17): asynchronous provider-side check. Triggered when the user
     * leaves the API key field. Calls a lightweight provider endpoint with a
     * short timeout and reflects the verdict in the status label.
     */
    private void runApiKeyApiCheck() {
        final JLabel status = getApiKeyStatusLabel();
        final String key      = (apiKeyText == null) ? "" : apiKeyText.getText().trim();
        final String provider = (String) getProviderComboBox().getSelectedItem();

        if ("lmStudio".equals(provider) || key.isEmpty()) {
            return;
        }

        String  urlStr;
        boolean useBearer;
        switch (provider) {
            case "openAi":
                urlStr    = "https://api.openai.com/v1/models";
                useBearer = true;
                break;
            case "gemini":
                urlStr    = "https://generativelanguage.googleapis.com/v1beta/models?key="
                        + URLEncoder.encode(key, StandardCharsets.UTF_8);
                useBearer = false;
                break;
            default:
                return;
        }

        status.setIcon(Messages.getImageIcon("Configuration.GPT.apikey.status.checking"));
        status.setText(Messages.getString("Configuration.GPT.apikey.status.checking"));
        status.setForeground(Color.GRAY);
        status.setToolTipText(null);

        final String  url        = urlStr;
        final boolean withBearer = useBearer;
        new Thread(() -> {
            int    code = -1;
            String err  = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (withBearer) {
                    conn.setRequestProperty("Authorization", "Bearer " + key);
                }
                code = conn.getResponseCode();
            } catch (Exception ex) {
                err = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            }
            final int    fCode = code;
            final String fErr  = err;
            SwingUtilities.invokeLater(() -> {
                if (fCode == 200) {
                    status.setIcon(Messages.getImageIcon("Configuration.GPT.apikey.status.ok"));
                    status.setText(Messages.getString("Configuration.GPT.apikey.status.ok"));
                    status.setForeground(new Color(0, 128, 0));
                    status.setToolTipText(Messages.getString("Configuration.GPT.apikey.status.tooltip.ok"));
                } else {
                    status.setIcon(Messages.getImageIcon("Configuration.GPT.apikey.status.invalid"));
                    status.setText(Messages.getString("Configuration.GPT.apikey.status.invalid"));
                    status.setForeground(Color.RED);
                    String tt = Messages.getString("Configuration.GPT.apikey.status.tooltip.invalid");
                    if (fErr != null)     tt += " - " + fErr;
                    else if (fCode > 0)   tt += " (HTTP " + fCode + ")";
                    status.setToolTipText(tt);
                }
            });
        }).start();
    }

    private JTextArea getPromptText() {
        if (promptText == null) {
            promptText = new JTextArea();
            promptText.setColumns(40);
            // WFC-US22 (#27): 4 rows so two prompt areas (T2P + P2T) fit comfortably.
            promptText.setRows(4);
            promptText.setLineWrap(true);
            promptText.setWrapStyleWord(true);
            promptText.setEnabled(true);
            // WFC-US9 (#12): tooltip explains the role of this text as an extension to the
            // base prompt that WoPeD assembles before sending the LLM request.
            promptText.setToolTipText(Messages.getString("Configuration.GPT.tool.tip.text.Title"));
            // The default value below is the full LLM instruction that is sent to the
            // webservice as the &prompt= parameter (the server forwards it to the model).
            // Prof. Freytag's "Zusatz-Prompt" / "Prompt Extension" vision treats this field
            // as an extension to a server-side base prompt, but until the server is updated
            // to provide its own base prompt we keep the complete instruction here so that
            // generation actually works.
            promptText.setText(
                    "Create a clearly structured and comprehensible continuous text from the given BPMN that is understandable for an uninformed reader. The text should be easy to read in the summary and contain all important content; if there are subdivided points, these are integrated into the text with suitable sentence beginnings in order to obtain a well-structured and easy-to-read text. Under no circumstances should the output contain sub-items or paragraphs, but should cover all processes in one piece!");
        }
        return promptText;
    }

    /** WFC-US22 (#27): editable T2P (Text -> Prozess) prompt template. */
    private JTextArea getPromptTextT2P() {
        if (promptTextT2P == null) {
            promptTextT2P = new JTextArea();
            promptTextT2P.setColumns(40);
            promptTextT2P.setRows(4);
            promptTextT2P.setLineWrap(true);
            promptTextT2P.setWrapStyleWord(true);
            promptTextT2P.setEnabled(true);
            promptTextT2P.setToolTipText(Messages.getString("Configuration.GPT.prompt.T2P.tooltip"));
        }
        return promptTextT2P;
    }

    private JScrollPane getPromptTextScrollPaneT2P() {
        JScrollPane sp = new JScrollPane(getPromptTextT2P());
        sp.setPreferredSize(new Dimension(520, 100));
        return sp;
    }

    public JCheckBox getRagOptionBox() {
        if (ragOptionBox == null) {
            ragOptionBox = new JCheckBox(Messages.getString("Configuration.GPT.rag.option"));
            ragOptionBox.setEnabled(true);
            ragOptionBox.setToolTipText(Messages.getString("Configuration.GPT.rag.option.tooltip"));
        }
        return ragOptionBox;
    }

    private JCheckBox getShowAgainBox() {
        if (showAgainBox == null) {
            showAgainBox = new JCheckBox(Messages.getString("Configuration.GPT.show.again.Title"));
            showAgainBox.setEnabled(true);
            // WFC-US9 (#12): the tooltip key Configuration.GPT.tool.tip.text.Title
            // describes the prompt text area, so it now lives on getPromptText().
        }
        return showAgainBox;
    }

    private WopedButton getResetButton() {
        if (resetButton == null) {
            resetButton = new WopedButton();
            resetButton.setText(Messages.getString("Configuration.GPT.standard.Title"));
            resetButton.setIcon(Messages.getImageIcon("Button.SetToDefault"));
            resetButton.setPreferredSize(new Dimension(200, 25));
            resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setDefaultValuesGPT();
                }
            });
        }
        return resetButton;
    }

    private WopedButton getFetchGPTModelsButton() {
        if (fetchGPTModelsButton == null) {
            fetchGPTModelsButton = new WopedButton();
            fetchGPTModelsButton.setText(Messages.getString("P2T.fetchmodels.button"));
            fetchGPTModelsButton.setIcon(Messages.getImageIcon("Action.Browser.Refresh"));
            fetchGPTModelsButton.setPreferredSize(new Dimension(200, 25));
            fetchGPTModelsButton.addActionListener(e -> fetchAndFillModels());
        }
        return fetchGPTModelsButton;
    }

    private WopedButton getCheckConnectionButton() {
        if (checkConnectionButton == null) {
            checkConnectionButton = new WopedButton();
            checkConnectionButton.setText(Messages.getString("Configuration.GPT.connection.Title"));
            checkConnectionButton.setIcon(Messages.getImageIcon("Button.TestConnection"));
            checkConnectionButton.setMnemonic(Messages.getMnemonic("Button.TestConnection"));
            checkConnectionButton.setPreferredSize(new Dimension(170, 25));
            checkConnectionButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    testGPTConnection();
                }
            });
        }
        return checkConnectionButton;
    }

    private void testGPTConnection() {
        String apiKey = apiKeyText.getText();
        String provider = (String) getProviderComboBox().getSelectedItem();
        String urlString;

        // Provider-specific endpoints — match those used by ApiHelper.fetchModels().
        switch (provider) {
            case "openAi":
                urlString = "https://api.openai.com/v1/models";
                break;
            case "gemini":
                urlString = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
                break;
            case "lmStudio":
                urlString = "http://localhost:1234/v1/models";
                break;
            default:
                urlString = "https://api.openai.com/v1/models";
                break;
        }

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // WFC-US5 (#6): bound the request so the dialog cannot freeze the UI thread.
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Bearer auth only for OpenAI; gemini carries the key in the URL, lmStudio is unauthenticated.
            if ("openAi".equals(provider)) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            connection.connect();

            int responseCode = connection.getResponseCode();
            String message;
            if (responseCode == 200) {
                message = "Connection successful to " + provider + " API!";
                JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                message = Messages.getString("Configuration.GPT.connection.failed.Title")
                        + responseCode + " (" + provider + ")";
                JOptionPane.showMessageDialog(this, message, "Connection Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this.getGPTPanel(),
                    Messages.getString("Configuration.GPT.connection.test.failed.Title")
                            + provider + ": " + e.getMessage(),
                    Messages.getString("Configuration.GPT.connection.test.Title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDefaultValuesGPT() {
        getProviderComboBox().setSelectedItem(ConfigurationManager.getStandardConfiguration().getLlmProvider());
        getApiKeyText().setText(ConfigurationManager.getStandardConfiguration().getGptApiKey());
        getShowAgainBox().setSelected(ConfigurationManager.getStandardConfiguration().getGptShowAgain());
        getPromptText().setText(ConfigurationManager.getStandardConfiguration().getGptPrompt());
        getPromptTextT2P().setText(ConfigurationManager.getStandardConfiguration().getGptPromptT2P());
    }

    private JComboBox<String> getModelComboBox() {
        if (modelComboBox == null) {
            modelComboBox = new JComboBox<>();
            modelComboBox.setEnabled(true);
            modelComboBox.setToolTipText("Select a model");
        }
        return modelComboBox;
    }

    private JCheckBox getUseBox() {
        if (useBox == null) {
            useBox = new JCheckBox(Messages.getString("Configuration.P2T.Label.Use"));
            useBox.setEnabled(true);
            useBox.setToolTipText("<html>" + Messages.getString("Configuration.P2T.Label.Use") + "</html>");
            CheckboxListener cbl = new CheckboxListener();
            useBox.addItemListener(cbl);
        }

        return useBox;
    }

    private JCheckBox getUseBox_T2P() {
        if (useBox == null) {
            useBox = new JCheckBox(Messages.getString("Configuration.P2T.Label.Use"));
            useBox.setEnabled(true);
            useBox.setToolTipText("<html>" + Messages.getString("Configuration.T2P.Label.Use") + "</html>");
            CheckboxListener cbl = new CheckboxListener();
            useBox.addItemListener(cbl);
        }

        return useBox;
    }

    private JLabel getManagerPathLabel() {
        if (managerPathLabel == null) {
            managerPathLabel = new JLabel(
                    "<html>" + Messages.getString("Configuration.P2T.Label.ServerURI") + "</html>");
            managerPathLabel.setHorizontalAlignment(JLabel.RIGHT);
        }
        return managerPathLabel;
    }

    private JLabel getManagerPathLabel_T2P() {
        if (managerPathLabel_T2P == null) {
            managerPathLabel_T2P = new JLabel(
                    "<html>" + Messages.getString("Configuration.T2P.Label.ServerURI") + "</html>");
            managerPathLabel_T2P.setHorizontalAlignment(JLabel.RIGHT);
        }
        return managerPathLabel_T2P;
    }

    private JTextField getManagerPathText() {
        if (managerPathText == null) {
            managerPathText = new JTextField();
            managerPathText.setColumns(40);
            managerPathText.setEnabled(true);
            managerPathText
                    .setToolTipText("<html>" + Messages.getString("Configuration.P2T.Label.ServerURI") + "</html>");
        }
        return managerPathText;
    }

    private JTextField getManagerPathText_T2P() {
        if (managerPathText_T2P == null) {
            managerPathText_T2P = new JTextField();
            managerPathText_T2P.setColumns(40);
            managerPathText_T2P.setEnabled(true);
            managerPathText_T2P
                    .setToolTipText("<html>" + Messages.getString("Configuration.T2P.Label.ServerURI") + "</html>");
        }
        return managerPathText_T2P;
    }

    private WopedButton getTestButton() {
        if (testButton == null) {
            testButton = new WopedButton();
            testButton.setText(Messages.getTitle("Button.TestConnection"));
            testButton.setIcon(Messages.getImageIcon("Button.TestConnection"));
            testButton.setMnemonic(Messages.getMnemonic("Button.TestConnection"));
            testButton.setPreferredSize(new Dimension(160, 25));
            testButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    testProcess2TextConnection();
                }
            });
        }

        return testButton;
    }

    private WopedButton getTestButton_T2P() {
        if (testButton_T2P == null) {
            testButton_T2P = new WopedButton();
            testButton_T2P.setText(Messages.getTitle("Button.TestConnection"));
            testButton_T2P.setIcon(Messages.getImageIcon("Button.TestConnection"));
            testButton_T2P.setMnemonic(Messages.getMnemonic("Button.TestConnection"));
            testButton_T2P.setPreferredSize(new Dimension(160, 25));
            testButton_T2P.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    testText2ProcessConnection();
                }
            });
        }

        return testButton_T2P;
    }

    private WopedButton getDefaultButton() {
        if (defaultButton == null) {
            defaultButton = new WopedButton();
            defaultButton.setText(Messages.getTitle("Button.SetToDefault"));
            defaultButton.setIcon(Messages.getImageIcon("Button.SetToDefault"));
            defaultButton.setPreferredSize(new Dimension(200, 25));
            defaultButton.addActionListener(e -> setDefaultValues());
        }
        return defaultButton;
    }

    private WopedButton getDefaultButton_T2P() {
        if (defaultButton_T2P == null) {
            defaultButton_T2P = new WopedButton();
            defaultButton_T2P.setText(Messages.getTitle("Button.SetToDefault"));
            defaultButton_T2P.setIcon(Messages.getImageIcon("Button.SetToDefault"));
            defaultButton_T2P.setPreferredSize(new Dimension(200, 25));
            defaultButton_T2P.addActionListener(e -> setDefaultValues_T2P());
        }
        return defaultButton_T2P;
    }

    private JTextField getServiceUrlText_LLM() {
        if (serviceUrlText_LLM == null) {
            serviceUrlText_LLM = new JTextField();
            serviceUrlText_LLM.setColumns(40);
            serviceUrlText_LLM.setEnabled(true);
            serviceUrlText_LLM
                    .setToolTipText("<html>" + Messages.getString("Configuration.T2P.Label.ServerHost") + "</html>");
        }
        return serviceUrlText_LLM;
    }

    private JLabel getServiceUrlLabel_LLM() {
        if (serviceUrlLabel_LLM == null) {
            serviceUrlLabel_LLM = new JLabel(
                    "<html>" + Messages.getString("Configuration.T2P.Label.ServerHost") + "</html>");
            serviceUrlLabel_LLM.setHorizontalAlignment(JLabel.RIGHT);
        }
        return serviceUrlLabel_LLM;
    }

    private JLabel getServicePortLabel_LLM() {
        if (servicePortLabel_LLM == null) {
            servicePortLabel_LLM = new JLabel(
                    "<html>" + Messages.getString("Configuration.T2P.Label.ServerPort") + "</html>");
            servicePortLabel_LLM.setHorizontalAlignment(JLabel.RIGHT);
        }
        return servicePortLabel_LLM;
    }

    private JTextField getServicePortText_LLM() {
        if (servicePortText_LLM == null) {
            servicePortText_LLM = new JTextField();
            servicePortText_LLM.setColumns(4);
            servicePortText_LLM.setEnabled(true);
            servicePortText_LLM
                    .setToolTipText("<html>" + Messages.getString("Configuration.T2P.Label.ServerPort") + "</html>");
        }
        return servicePortText_LLM;
    }

    private JLabel getServiceUriLabel_LLM() {
        if (serviceUriLabel_LLM == null) {
            serviceUriLabel_LLM = new JLabel(Messages.getString("Configuration.T2P.Label.ServerURI"));
            serviceUriLabel_LLM.setHorizontalAlignment(JLabel.RIGHT);
        }
        return serviceUriLabel_LLM;
    }

    private JTextField getServiceUriText_LLM() {
        if (serviceUriText_LLM == null) {
            serviceUriText_LLM = new JTextField();
            serviceUriText_LLM.setColumns(40);
            serviceUriText_LLM.setEnabled(true);
            serviceUriText_LLM
                    .setToolTipText("<html>" + Messages.getString("Configuration.T2P.Label.ServerURI") + "</html>");
        }
        return serviceUriText_LLM;
    }

    private WopedButton getTestButton_LLM() {
        if (testButton_LLM == null) {
            testButton_LLM = new WopedButton();
            testButton_LLM.setText(Messages.getTitle("Button.TestConnection"));
            testButton_LLM.setIcon(Messages.getImageIcon("Button.TestConnection"));
            testButton_LLM.setMnemonic(Messages.getMnemonic("Button.TestConnection"));
            testButton_LLM.setPreferredSize(new Dimension(160, 25));
            testButton_LLM.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    testLLMConnection();
                }
            });
        }
        return testButton_LLM;
    }

    private WopedButton getDefaultButton_LLM() {
        if (defaultButton_LLM == null) {
            defaultButton_LLM = new WopedButton();
            defaultButton_LLM.setText(Messages.getTitle("Button.SetToDefault"));
            defaultButton_LLM.setIcon(Messages.getImageIcon("Button.SetToDefault"));
            defaultButton_LLM.setPreferredSize(new Dimension(200, 25));
            defaultButton_LLM.addActionListener(e -> setDefaultValues_LLM());
        }
        return defaultButton_LLM;
    }

    private void testLLMConnection() {
        String rawHost = getServiceUrlText_LLM().getText().trim();
        String rawPort = getServicePortText_LLM().getText().trim();
        String rawPath = getServiceUriText_LLM().getText().trim();
        String[] arg = { rawHost, "" };

        try {
            // Respect an explicit scheme in the host field; default to https for port 443
            // otherwise http.
            boolean hostHasScheme = rawHost.startsWith("http://") || rawHost.startsWith("https://");
            String scheme = hostHasScheme ? "" : (":443".equals(":" + rawPort) ? "https://" : "http://");
            String hostPart = rawHost;
            if (!hostHasScheme) {
                hostPart = rawHost;
            }

            String portPart = rawPort.isEmpty() ? "" : ":" + rawPort;

            // Normalize path and append test endpoint only once.
            String normalizedPath = rawPath.isEmpty() ? "" : (rawPath.startsWith("/") ? rawPath : "/" + rawPath);
            if (!normalizedPath.endsWith("/test_connection")) {
                normalizedPath = normalizedPath + (normalizedPath.endsWith("/") ? "" : "/") + "test_connection";
            }

            URL url = new URL(scheme + hostPart + portPart + normalizedPath);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setConnectTimeout(10000);
            httpConnection.setReadTimeout(10000);

            int responseCode = httpConnection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
                String response = reader.readLine();
                reader.close();

                if (response != null && response.contains("Successful")) {
                    arg[1] = "LLM";
                    JOptionPane.showMessageDialog(
                            this.getSettingsPanel_LLM(),
                            Messages.getString("Paraphrasing.Webservice.Success.Message", arg),
                            Messages.getString("Paraphrasing.Webservice.Success.Title"),
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            throw new IOException("Server returned unexpected response: " + responseCode);

        } catch (javax.net.ssl.SSLHandshakeException ex) {
            String errorMsg = Messages.getString("Paraphrasing.Webservice.Error.WebserviceException.Message", arg)
                    + "\n\nSSL Certificate Error: " + ex.getMessage()
                    + "\n\nPossible causes:"
                    + "\n- Certificate not trusted (self-signed or missing CA)"
                    + "\n- Certificate expired or not yet valid"
                    + "\n- Hostname mismatch"
                    + "\n- Java version: " + System.getProperty("java.version");
            JOptionPane.showMessageDialog(
                    this.getSettingsPanel_LLM(),
                    errorMsg,
                    Messages.getString("Paraphrasing.Webservice.Error.Title"),
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this.getSettingsPanel_LLM(),
                    Messages.getString("Paraphrasing.Webservice.Error.WebserviceException.Message", arg)
                            + "\n\n" + ex.getMessage(),
                    Messages.getString("Paraphrasing.Webservice.Error.Title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDefaultValues_LLM() {
        getServiceUrlText_LLM().setText(ConfigurationManager.getStandardConfiguration().getT2PLlmServiceHost());
        getServicePortText_LLM().setText("" + ConfigurationManager.getStandardConfiguration().getT2PLlmServicePort());
        getServiceUriText_LLM().setText("" + ConfigurationManager.getStandardConfiguration().getT2PLlmServiceUri());
    }

    private void testProcess2TextConnection() {
        URL url = null;
        String port = getServerPortText().getText().isEmpty() ? "" : ":" + getServerPortText().getText();
        String protocol = getServerPortText().getText().isEmpty() || !port.equals(":443") ? "http://" : "https://";
        String host = getServerURLText().getText().trim();
        String connection = protocol.trim() + host.trim() + port.trim()
                + getManagerPathText().getText().trim();
        String[] arg = { connection, "" };

        try {
            url = new URL(connection);
            URLConnection urlConnection = url.openConnection();

            if (urlConnection.getContent() != null) {
                arg[1] = "P2T";
                JOptionPane.showMessageDialog(this.getSettingsPanel(),
                        Messages.getString("Paraphrasing.Webservice.Success.Message", arg),
                        Messages.getString("Paraphrasing.Webservice.Success.Title"), JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this.getSettingsPanel(),
                    Messages.getString("Paraphrasing.Webservice.Error.WebserviceException.Message", arg),
                    Messages.getString("Paraphrasing.Webservice.Error.Title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void testText2ProcessConnection() {
        URL url;
        String port = getServerPortText_T2P().getText().isEmpty() ? "" : ":" + getServerPortText_T2P().getText();
        String protocol = getServerPortText_T2P().getText().isEmpty() || !port.equals(":443") ? "http://" : "https://";
        String host = getServerURLText_T2P().getText().trim();
        String connection = protocol.trim() + host.trim() + port.trim()
                + getManagerPathText_T2P().getText().trim();
        String[] arg = { connection, "" };

        try {
            url = new URL(connection);
            URLConnection urlConnection = url.openConnection();
            if (urlConnection.getContent() != null) {
                arg[1] = "T2P";
                JOptionPane.showMessageDialog(this.getSettingsPanel_T2P(),
                        Messages.getString("Paraphrasing.Webservice.Success.Message", arg),
                        Messages.getString("Paraphrasing.Webservice.Success.Title"), JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this.getSettingsPanel_T2P(),
                    Messages.getString("Paraphrasing.Webservice.Error.WebserviceException.Message", arg),
                    Messages.getString("Paraphrasing.Webservice.Error.Title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDefaultValues() {
        getServerURLText().setText(ConfigurationManager.getStandardConfiguration().getProcess2TextServerHost());
        getManagerPathText().setText(ConfigurationManager.getStandardConfiguration().getProcess2TextServerURI());
        getServerPortText().setText("" + ConfigurationManager.getStandardConfiguration().getProcess2TextServerPort());
    }

    private void setDefaultValues_T2P() {
        getServerURLText_T2P().setText(ConfigurationManager.getStandardConfiguration().getText2ProcessServerHost());
        getManagerPathText_T2P().setText(ConfigurationManager.getStandardConfiguration().getText2ProcessServerURI());
        getServerPortText_T2P()
                .setText("" + ConfigurationManager.getStandardConfiguration().getText2ProcessServerPort());
    }

    class CheckboxListener implements ItemListener {
        public void itemStateChanged(ItemEvent ie) {
            JCheckBox jcb = (JCheckBox) ie.getSource();
            if (jcb == useBox) {
                getSettingsPanel().setVisible(jcb.isSelected());
                getSettingsPanel_T2P().setVisible(jcb.isSelected());
                getGPTPanel().setVisible(jcb.isSelected());
                getSettingsPanel_LLM().setVisible(jcb.isSelected());
            }
        }
    }

    private JLabel getServerURLLabel() {
        if (serverURLLabel == null) {
            serverURLLabel = new JLabel(
                    "<html>" + Messages.getString("Configuration.P2T.Label.ServerHost") + "</html>");
            serverURLLabel.setHorizontalAlignment(JLabel.RIGHT);
        }
        return serverURLLabel;
    }

    private JLabel getServerURLLabel_T2P() {
        if (serverURLLabel_T2P == null) {
            serverURLLabel_T2P = new JLabel(
                    "<html>" + Messages.getString("Configuration.T2P.Label.ServerHost") + "</html>");
            serverURLLabel_T2P.setHorizontalAlignment(JLabel.RIGHT);
        }
        return serverURLLabel_T2P;
    }

    private JLabel getServerPortLabel() {
        if (serverPortLabel == null) {
            serverPortLabel = new JLabel(
                    "<html>" + Messages.getString("Configuration.P2T.Label.ServerPort") + "</html>");
            serverPortLabel.setHorizontalAlignment(JLabel.RIGHT);
        }
        return serverPortLabel;
    }

    private JLabel getServerPortLabel_T2P() {
        if (serverPortLabel_T2P == null) {
            serverPortLabel_T2P = new JLabel(
                    "<html>" + Messages.getString("Configuration.T2P.Label.ServerPort") + "</html>");
            serverPortLabel_T2P.setHorizontalAlignment(JLabel.RIGHT);
        }
        return serverPortLabel_T2P;
    }

    private JTextField getServerPortText() {
        if (serverPortText == null) {
            serverPortText = new JTextField();
            serverPortText.setColumns(4);
            serverPortText.setEnabled(true);
            serverPortText
                    .setToolTipText("<html>" + Messages.getString("Configuration.P2T.Label.ServerPort") + "</html>");
        }
        return serverPortText;
    }

    private JTextField getServerPortText_T2P() {
        if (serverPortText_T2P == null) {
            serverPortText_T2P = new JTextField();
            serverPortText_T2P.setColumns(4);
            serverPortText_T2P.setEnabled(true);
            serverPortText_T2P
                    .setToolTipText("<html>" + Messages.getString("Configuration.T2P.Label.ServerPort") + "</html>");
        }
        return serverPortText_T2P;
    }

    /** Convenience: fetch with error dialogs (used by the explicit "GPT-Modelle abrufen" button). */
    private void fetchAndFillModels() {
        fetchAndFillModels(false);
    }

    /**
     * Fetch the model list from the configured provider and fill the model
     * combobox. WFC-US6 (#7): callers that fire on dialog open / provider change
     * pass {@code silentOnError = true} so background fetches don't surprise the
     * user with error popups.
     */
    private void fetchAndFillModels(boolean silentOnError) {
        new Thread(() -> {
            try {
                // Provider aus der ComboBox nehmen
                String provider = (String) getProviderComboBox().getSelectedItem();
                if (provider == null || provider.isEmpty()) {
                    provider = "openAi"; // Default fallback
                }

                String apiKey = "";
                // Für LM Studio wird kein API Key benötigt
                if (!"lmStudio".equals(provider)) {
                    apiKey = apiKeyText.getText();
                }

                modelComboBox.removeAllItems(); // Zuerst alte Modelle entfernen
                List<String> models = ApiHelper.fetchModels(apiKey, provider);
                SwingUtilities.invokeLater(() -> {
                    for (String model : models) {
                        modelComboBox.addItem(model);
                    }
                    modelComboBox.setSelectedItem(ConfigurationManager.getConfiguration().getGptModel());
                });
            } catch (IOException | ParseException e) {
                if (silentOnError) {
                    org.woped.core.utilities.LoggerManager.warn(
                            org.woped.editor.Constants.EDITOR_LOGGER,
                            "Silent model fetch failed: " + e.getMessage());
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                Messages.getString("P2T.exception.fail.fetch.models") + e.getMessage(),
                                Messages.getString("P2T.exception.fetch.models"), JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        }).start();
    }

    /**
     * WFC-US6 (#7): kick off background API-key validation and model fetch when
     * the dialog opens so the user sees the verdict + model list without having
     * to click anything. Failures are logged silently — no surprise popups.
     */
    private void triggerInitialChecks() {
        runApiKeyApiCheck();
        String key = getApiKeyText().getText();
        String provider = (String) getProviderComboBox().getSelectedItem();
        boolean canFetch = "lmStudio".equals(provider)
                || (key != null && !key.trim().isEmpty());
        if (canFetch) {
            fetchAndFillModels(true);
        }
    }

    /**
     * Load custom truststore from resources to support GEANT CA and other certificates
     * not in the default Java truststore. This ensures the JAR works on any system
     * without requiring manual certificate imports.
     */
    private static void loadCustomTruststore() {
        try {
            // Try to load bundled truststore from resources
            InputStream truststoreStream = ConfNLPToolsPanel.class
                    .getResourceAsStream("/woped-truststore.jks");
            
            if (truststoreStream != null) {
                // Load the custom truststore
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                char[] password = "woped123".toCharArray(); // Consider externalizing this
                trustStore.load(truststoreStream, password);
                truststoreStream.close();

                // Initialize TrustManager with custom truststore
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);

                // Create SSL context with custom trust managers
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                
                // Set as default for all HTTPS connections
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                
                System.out.println("Custom truststore loaded successfully");
            } else {
                // Fallback: merge with system truststore
                System.out.println("Custom truststore not found, using system default");
            }
        } catch (Exception e) {
            System.err.println("Failed to load custom truststore: " + e.getMessage());
            // Continue with default truststore - connection may fail but app won't crash
        }
    }

}
