package uk.anbu.samples.filewatcher;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WatcherFrame {
    private final ConfigManager configManager;
    private JFrame frame;
    private JTextField directoryField;
    private JCheckBox monitorSubdirectoriesCheckbox;
    private JTextField workingDirField;
    private JTextField globPatternsField;
    private JTextField commandField;
    private JCheckBox watchActiveCheckbox;

    public WatcherFrame(Runnable startWatching, Runnable stopWatching, ConfigManager configManager) {
        this.configManager = configManager;

        frame = new JFrame("Directory Watcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Directory input field and subdirectories checkbox
        directoryField = new JTextField(30);
        monitorSubdirectoriesCheckbox = new JCheckBox("Monitor Subdirectories");
        JPanel directoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        directoryPanel.add(new JLabel("Directory to Monitor:"));
        directoryPanel.add(directoryField);
        directoryPanel.add(monitorSubdirectoriesCheckbox);

        // Working directory input field
        workingDirField = new JTextField(30);
        JPanel workingDirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        workingDirPanel.add(new JLabel("Working Directory:"));
        workingDirPanel.add(workingDirField);

        // Glob pattern input field
        globPatternsField = new JTextField(30);
        JPanel globPatternPanel = new JPanel(new FlowLayout());
        globPatternPanel.add(new JLabel("File Glob Pattern:"));
        globPatternPanel.add(globPatternsField);

        // Command input field
        commandField = new JTextField(30);
        JPanel commandPanel = new JPanel(new FlowLayout());
        commandPanel.add(new JLabel("Command to Trigger:"));
        commandPanel.add(commandField);

        // Watch Active Checkbox
        watchActiveCheckbox = new JCheckBox("Watch Active");
        JPanel watchPanel = new JPanel(new FlowLayout());
        watchPanel.add(watchActiveCheckbox);

        // Panel to group all input fields
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(directoryPanel);
        inputPanel.add(workingDirPanel);
        inputPanel.add(globPatternPanel);
        inputPanel.add(commandPanel);
        inputPanel.add(watchPanel);

        // Add components to the frame
        frame.add(inputPanel, BorderLayout.NORTH);

        // Watch Active Checkbox listener
        watchActiveCheckbox.addActionListener(e -> {
            if (watchActiveCheckbox.isSelected()) {
                setInputFieldsEditable(false);
                saveCurrentConfig();
                startWatching.run();
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

    public void saveCurrentConfig() {
        configManager.saveConfig(new ConfigManager.Config(
                directoryField.getText(),
                workingDirField.getText(),
                commandField.getText(),
                getGlobPatterns(),
                monitorSubdirectoriesCheckbox.isSelected()
        ));
    }

    private void setInputFieldsEditable(boolean editable) {
        directoryField.setEditable(editable);
        globPatternsField.setEditable(editable);
        commandField.setEditable(editable);
        workingDirField.setEditable(editable);
        monitorSubdirectoriesCheckbox.setEnabled(editable);

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

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public String getDirectory() {
        return directoryField.getText();
    }

    public List<String> getGlobPatterns() {
        return Arrays.stream(globPatternsField.getText().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    public String getWorkingDirectory() {
        return workingDirField.getText();
    }

    public String getCommand() {
        return commandField.getText();
    }

    public boolean isMonitorSubdirectories() {
        return monitorSubdirectoriesCheckbox.isSelected();
    }
}
