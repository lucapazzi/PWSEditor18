package pws.editor;

import assembly.Assembly;
import pws.PWSState;
import pws.editor.semantics.Semantics;
import smalgebra.BasicStateProposition;

import javax.swing.*;
import java.awt.*;

public class ConstraintsEditorDialog extends JDialog {
    private JTextArea textArea;
    private JButton applyButton, cancelButton;
    private PWSState state;  // the state whose constraint semantics we're editing
    private Assembly assembly; // Added field

    public ConstraintsEditorDialog(PWSState state, Assembly assembly) { // Modified constructor
        this.state = state;
        this.assembly = assembly;
        setModal(true);
        setTitle("Edit Constraints Semantics");
        textArea = new JTextArea(10, 30);
        // Prepopulate with current constraints semantics
        textArea.setText(getConstraintsTextFromState(state));

        applyButton = new JButton("Apply");
        cancelButton = new JButton("Cancel");

        applyButton.addActionListener(e -> {
            String text = textArea.getText();
            Semantics newConstraints = parseConfigurations(text);
            state.setConstraintsSemantics(newConstraints);
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private Semantics parseConfigurations(String text) {
        // For each non-empty line in text, parse the configuration, then union them
        Semantics result = Semantics.bottom(assembly); // Modified call
        for (String line : text.split("\\n")) {
            if (!line.trim().isEmpty()) {
                // Here, parse each line into a Semantics object.
                Semantics confSem = parseConfigurationLine(line.trim());
                result = result.OR(confSem);
            }
        }
        return result;
    }

    private String getConstraintsTextFromState(PWSState state) {
        // Convert the current constraints semantics into editable text lines
        StringBuilder sb = new StringBuilder();
        // Each configuration prints as "(m1.s1,m2.s2,...)"
        for (Object cfg : state.getConstraintsSemantics().getConfigurations()) {
            String s = cfg.toString();
            // strip surrounding parentheses if present
            if (s.startsWith("(") && s.endsWith(")")) {
                s = s.substring(1, s.length() - 1);
            }
            // ensure comma + space separation
            s = s.replace(",", ", ");
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    private Semantics parseConfigurationLine(String line) {
        // Remove surrounding parentheses if present
        if (line.startsWith("(") && line.endsWith(")")) {
            line = line.substring(1, line.length() - 1);
        }
        String[] pairs = line.split(",");
        // Start with universal (top) semantics
        Semantics configSem = Semantics.top(assembly);
        for (String pair : pairs) {
            String p = pair.trim();
            String machine = null, stateName = null;
            // support "machine:state" or "machine.state"
            if (p.contains(":")) {
                String[] parts = p.split(":", 2);
                machine = parts[0].trim();
                stateName = parts[1].trim();
            } else if (p.contains(".")) {
                String[] parts = p.split("\\.", 2);
                machine = parts[0].trim();
                stateName = parts[1].trim();
            }
            if (machine != null && stateName != null) {
                BasicStateProposition bsp = new BasicStateProposition(machine, stateName);
                configSem = configSem.AND(bsp.toSemantics(assembly));
            }
        }
        return configSem;
    }
}