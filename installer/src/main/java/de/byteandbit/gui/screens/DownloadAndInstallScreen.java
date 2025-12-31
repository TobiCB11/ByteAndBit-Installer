package de.byteandbit.gui.screens;

import de.byteandbit.Constants;
import de.byteandbit.Util;
import de.byteandbit.api.AgentApi;
import de.byteandbit.api.ProductApi;
import de.byteandbit.data.GameInstance;
import de.byteandbit.gui.Gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import static de.byteandbit.Util.*;


public class DownloadAndInstallScreen implements Screen {
    private JLabel majorStatusLabel;
    private JLabel minorStatusLabel;

    private JPanel panel;

    @lombok.Setter
    private GameInstance selectedInstance;
    @lombok.Setter
    private String selecteScope;

    public DownloadAndInstallScreen(GameInstance selectedInstance, String selecteScope) {
        this.selectedInstance = selectedInstance;
        this.selecteScope = selecteScope;
    }

    @Override
    public JPanel getScreen() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        try {
            BufferedImage logo = ImageIO.read(Constants.BAB_LOGO);
            Rectangle screenSize = Constants.PROGRAM_GEOMETRY;
            int targetWidth = screenSize.width / 2;
            int targetHeight = screenSize.height / 2;
            double aspectRatio = (double) logo.getWidth() / logo.getHeight();
            if (targetWidth / aspectRatio > targetHeight) {
                targetWidth = (int) (targetHeight * aspectRatio);
            } else {
                targetHeight = (int) (targetWidth / aspectRatio);
            }

            Image scaledLogo = logo.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(Box.createVerticalGlue());
            panel.add(logoLabel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        majorStatusLabel = new JLabel("");
        majorStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        majorStatusLabel.setFont(majorStatusLabel.getFont().deriveFont(Font.BOLD, 16f));

        minorStatusLabel = new JLabel("");
        minorStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(majorStatusLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(minorStatusLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    @Override
    public String identifier() {
        return "downloading";
    }

    public void setMajorStatus(String status) {
        SwingUtilities.invokeLater(() -> majorStatusLabel.setText(status));
    }

    public void setMinorStatus(String status) {
        SwingUtilities.invokeLater(() -> minorStatusLabel.setText(status));
    }


    private String getCoreName(String fileName) {
        String corename = fileName.replaceAll("[^A-z]", "").toLowerCase();
        for (String scope : ProductApi.getInstance().getAvailableScopes()) {
            corename = corename.replace(scope, "");
        }
        return corename;
    }


    @Override
    public void onOpen() {
        new Thread(() -> {
            try {
                setMajorStatus(String.format(uiText("DOWNLOADING_ADDON"), ProductApi.getInstance().getProductName()));
                setMinorStatus(uiText("GETTING_DOWNLOAD_LINK"));
                ui_wait();
                ProductApi.DownloadResponse downloadResponse = ProductApi.getInstance().getDownloadLink(selectedInstance, selecteScope);
                File modsFolder = new File(selectedInstance.getGameDir(), "mods");
                File downloaded = Util.downloadFileToFolder(downloadResponse.link, modsFolder, Util.uiThrottle((percent) -> {
                    setMinorStatus(percent + "%");
                }));
                ui_wait();

                String coreName = getCoreName(downloaded.getName());
                setMajorStatus(uiText("DOWNLOADING_LICENSE"));
                setMinorStatus("0%");
                ui_wait();
                File configFolder = new File(selectedInstance.getGameDir(), "config");
                Util.downloadFileToFolder(ProductApi.getInstance().getLicenseDownloadUrl(), configFolder, Util.uiThrottle((percent) -> {
                    setMinorStatus(percent + "%");
                }));

                setMajorStatus(uiText("CHECKING_FOR_OLD_VERSIONS"));
                setMinorStatus("");
                ArrayList<String> toRemove = new ArrayList<>();

                for (File f : Objects.requireNonNull(modsFolder.listFiles())) {
                    if (f.getName().equals(downloaded.getName())) continue;
                    if (getCoreName(f.getName()).equals(coreName)) {
                        toRemove.add(f.getAbsolutePath());
                        setMinorStatus(String.format(uiText("FOUND_OLD_VERSION"), f.getName()));
                        if (!tryDelete(f)) toRemove.add(f.getAbsolutePath());
                        ui_wait();
                        ui_wait();
                    }
                }
                if (!toRemove.isEmpty()) {
                    // add to shutdown remove hook
                    setMinorStatus("");
                    setMajorStatus(uiText("CLEANUP_OLD_VERSIONS"));
                    ui_wait();

                    try {
                        AgentApi.attach_addFileDeleteHooks(selectedInstance.getPid(), toRemove);
                    } catch (Exception ignored) {
                    } // this will always report that it didnt manage to attach for some reason.
                }
                setMajorStatus(uiText("INSTALLATION_COMPLETE"));
                setMinorStatus(uiText("RESTART_GAME"));
            } catch (IOException e) {
                e.printStackTrace();
                Gui.getInstance().errorAndExit(uiText("INSTALLATION_FAILED_ERROR"), e);
            }
        }).start();
    }

    @Override
    public void onClose() {

    }
}
