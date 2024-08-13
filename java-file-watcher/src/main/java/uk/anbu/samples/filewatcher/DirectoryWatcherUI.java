package uk.anbu.samples.filewatcher;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DirectoryWatcherUI {
    private final ConfigManager configManager;
    private final JFrame frame;
    private final JTextField directoryField;
    private final JCheckBox monitorSubdirectoriesCheckbox;
    private final JCheckBox consolidateChangesCheckbox;
    private final JTextField workingDirField;
    private final JTextField globPatternsField;
    private final JTextField commandField;
    private final JCheckBox watchActiveCheckbox;
    public DirectoryWatcherUI(Runnable startWatching, Runnable stopWatching, ConfigManager configManager) {
        this.configManager = configManager;

        frame = new JFrame("Directory Watcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addLabelAndField(mainPanel, gbc, "Directory to Monitor", directoryField = new JTextField(30));
        addLabelAndField(mainPanel, gbc, "Working Directory", workingDirField = new JTextField(30));
        addLabelAndField(mainPanel, gbc, "File Glob Pattern", globPatternsField = new JTextField(30));
        addLabelAndField(mainPanel, gbc, "Command to Trigger", commandField = new JTextField(30));

        // Checkboxes
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monitorSubdirectoriesCheckbox = new JCheckBox("Monitor Subdirectories");
        consolidateChangesCheckbox = new JCheckBox("Consolidate Changes");
        checkboxPanel.add(monitorSubdirectoriesCheckbox);
        checkboxPanel.add(consolidateChangesCheckbox);
        mainPanel.add(checkboxPanel, gbc);

        // Watch Active Checkbox
        gbc.gridy++;
        watchActiveCheckbox = new JCheckBox("Watch Active");
        mainPanel.add(watchActiveCheckbox, gbc);

        frame.add(mainPanel, BorderLayout.CENTER);

        watchActiveCheckbox.addActionListener(e -> {
            if (watchActiveCheckbox.isSelected()) {
                if (validateDirectories()) {
                    setInputFieldsEditable(false);
                    saveCurrentConfig();
                    startWatching.run();
                } else {
                    watchActiveCheckbox.setSelected(false);
                }
            } else {
                setInputFieldsEditable(true);
                stopWatching.run();
            }
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveCurrentConfig();
            }
        });
    }

    private void addLabelAndField(JPanel panel, GridBagConstraints gbc, String labelText, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    public void saveCurrentConfig() {
        var config = ConfigManager.Config.builder()
                .watchedDirectory(directoryField.getText())
                .workingDirectory(workingDirField.getText())
                .command(commandField.getText())
                .globPatterns(getGlobPatterns())
                .monitorSubdirectories(monitorSubdirectoriesCheckbox.isSelected())
                .consolidateChanges(consolidateChangesCheckbox.isSelected())
                .build();
        configManager.saveConfig(config);
    }

    private boolean validateDirectories() {
        String watchedDir = directoryField.getText().trim();
        String workingDir = workingDirField.getText().trim();

        StringBuilder errorMessage = new StringBuilder();

        if (!isValidDirectory(watchedDir)) {
            errorMessage.append("The watched directory is not valid or doesn't exist.\n");
        }

        if (!workingDir.isEmpty() && !isValidDirectory(workingDir)) {
            errorMessage.append("The working directory is not valid or doesn't exist.\n");
        }

        if (!errorMessage.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    errorMessage.toString(),
                    "Invalid Directory",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private boolean isValidDirectory(String path) {
        if (path.isEmpty()) {
            return false;
        }
        File dir = new File(path);
        return dir.exists() && dir.isDirectory();
    }

    private void setInputFieldsEditable(boolean editable) {
        directoryField.setEditable(editable);
        globPatternsField.setEditable(editable);
        commandField.setEditable(editable);
        workingDirField.setEditable(editable);
        monitorSubdirectoriesCheckbox.setEnabled(editable);
        consolidateChangesCheckbox.setEnabled(editable);

        Color backgroundColor = editable ? Color.WHITE : Color.LIGHT_GRAY;
        directoryField.setBackground(backgroundColor);
        workingDirField.setBackground(backgroundColor);
        globPatternsField.setBackground(backgroundColor);
        commandField.setBackground(backgroundColor);
    }

    void show() {
        var config = configManager.loadConfig();

        directoryField.setText(config.watchedDirectory());
        globPatternsField.setText(String.join(", ", config.globPatterns()));
        workingDirField.setText(config.workingDirectory());
        commandField.setText(config.command());
        monitorSubdirectoriesCheckbox.setSelected(config.monitorSubdirectories());
        consolidateChangesCheckbox.setSelected(config.consolidateChanges());

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public List<String> getGlobPatterns() {
        return Arrays.stream(globPatternsField.getText().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public ConfigManager.Config config() {
        return ConfigManager.Config.builder()
                .watchedDirectory(directoryField.getText())
                .workingDirectory(workingDirField.getText())
                .command(commandField.getText())
                .globPatterns(getGlobPatterns())
                .monitorSubdirectories(monitorSubdirectoriesCheckbox.isSelected())
                .consolidateChanges(consolidateChangesCheckbox.isSelected())
                .build();
    }
}
