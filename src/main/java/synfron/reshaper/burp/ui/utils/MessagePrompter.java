package synfron.reshaper.burp.ui.utils;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessagePrompter {
    private static Map<String, JDialog> dialogMap = new HashMap<>();

    public static void createTextAreaDialog(String id, String title, String description, Consumer<String> valueHandler) {
        JPanel container = new JPanel(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(new EmptyBorder(10,0,0,0));
        JTextPane inputText = new JTextPane();
        scrollPane.setViewportView(inputText);
        scrollPane.setPreferredSize(new Dimension(320, 160));

        container.add(new JLabel(description), BorderLayout.PAGE_START);
        container.add(scrollPane, BorderLayout.CENTER);

        JOptionPane optionPane = new JOptionPane(container, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(title);
        dialog.setResizable(true);
        dialogMap.put(id, dialog);

        optionPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, event -> {
            if ((int)optionPane.getValue() == JOptionPane.OK_OPTION) {
                valueHandler.accept(inputText.getText());
            }
        });

        dialog.setModal(false);
        dialog.setVisible(true);
    }

    public static void dismiss(String id) {
        JDialog dialog = dialogMap.get(id);
        if (dialog != null) {
            dialog.dispose();
        }
    }
}
