package de.byteandbit.gui.screens;

import de.byteandbit.api.ProductApi;
import de.byteandbit.gui.Gui;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.io.File;

import static de.byteandbit.Util.uiText;
import static de.byteandbit.Util.ui_wait;

/**
 * description missing.
 */
public class LicenseScreen implements Screen {
    private JPanel panel;
    private JTextField licenseField;
    private JButton verifyButton;
    private static final String license_prefix="LION-";

    @Override
    public JPanel getScreen() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel licenseLabel = new JLabel(uiText("ENTER_LICENSE_KEY"));
        // License label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(licenseLabel, gbc);

        // License text field
        licenseField = new JTextField(30);
        licenseField.setDocument(new LicenseDocument());
        licenseField.setText(license_prefix);
        licenseField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        licenseField.setHorizontalAlignment(JTextField.LEFT);
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(licenseField, gbc);

        // Verify button
        verifyButton = new JButton(uiText("VERIFY_LICENSE"));
        verifyButton.addActionListener(e -> verifyLicense());
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(verifyButton, gbc);

        return panel;
    }

    private void verifyLicense() {
        String license = licenseField.getText().trim();
        if (ProductApi.getInstance().setLicense(license)) {
            if (ProductApi.getInstance().isLegacyLicense()) {
                JOptionPane.showMessageDialog(panel, uiText("LEGACY_LICENSE"), uiText("INVALID_LICENSE_TITLE"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (ProductApi.getInstance().noUsersRegistered()) {
                Gui.getInstance().showScreen(new LicenseNeedsUserScreen());
            } else {
                Gui.getInstance().showScreen(new GameSelectScreen());
            }
        } else {
            JOptionPane.showMessageDialog(panel, uiText("INVALID_LICENSE"), uiText("INVALID_LICENSE_TITLE"), JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public String identifier() {
        return "license";
    }

    @Override
    public void onOpen() {
        // check if license file is in directory with the installer. If so, load and verify license with a small animation.
        new Thread(() -> {
            File[] files = new File(".").listFiles((dir, name) -> name.endsWith(".raaar"));
            if (files != null && files.length > 0) {
                SwingUtilities.invokeLater(() -> {
                    licenseField.setText(files[0].getName().replace(".raaar", ""));
                    licenseField.setEnabled(false);
                    verifyButton.setEnabled(false);
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    new Thread(() -> {
                        ui_wait();
                        ui_wait();
                        verifyLicense();
                        SwingUtilities.invokeLater(() -> {
                            panel.setCursor(Cursor.getDefaultCursor());
                            verifyButton.setEnabled(true);
                            licenseField.setEnabled(true);
                        });
                    }).start();
                });
            }
        }).start();

    }

    @Override
    public void onClose() {

    }

    /**
     * Sanitize the license input field to start with license_prefix and truncate to MAX_LENGTH.
     */
    private static class LicenseDocument extends PlainDocument {
        private static final int MAX_LENGTH = 35;

        @Override
        public void insertString(int offset, String str, AttributeSet attr)
                throws BadLocationException {
            if (str == null || str.isEmpty()) {
                return;
            }

            String current = getText(0, getLength());
            String combined;

            if (current.isEmpty()) {
                if (!str.startsWith(license_prefix)) {
                    str = license_prefix + str;
                }
                combined = str;
            } else {
                combined = new StringBuilder(current).insert(offset, str).toString();
                if (!combined.startsWith(license_prefix)) {
                    return;
                }
            }

            if (combined.length() > MAX_LENGTH) {
                combined = combined.substring(0, MAX_LENGTH);
            }

            String formatted = format(combined);

            super.remove(0, getLength());
            super.insertString(0, formatted, attr);
        }

        @Override
        public void remove(int offset, int length) throws BadLocationException {
            String current = getText(0, getLength());

            // Ensure we never remove the prefix
            if (offset < license_prefix.length()) {
                if (offset + length <= license_prefix.length()) {
                    return;
                } else {
                    length = offset + length - license_prefix.length();
                    offset = license_prefix.length();
                }
            }

            String combined = new StringBuilder(current)
                    .delete(offset, offset + length)
                    .toString();

            String formatted = format(combined);

            super.remove(0, getLength());
            super.insertString(0, formatted, null);
        }

        private String format(String s) {
            if(s.startsWith(license_prefix + license_prefix)){
                s = s.substring(license_prefix.length());
            }
            return s.toUpperCase().replaceAll("[^A-Z0-9-]", "");
        }
    }
}
