package com.miapp.mediaserver;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class ServerConfiguredController {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfiguredController.class);
    private TrayIcon trayIcon;
    private String token;
    private Stage stage; // Guardamos referencia al Stage
    private Scene serverRegistrationScene; // Guardamos la escena para restaurarla

    @FXML private Button finishButton;

    public void setToken(String token) {
        this.token = token;
    }

    @FXML
    private void initialize() {
        // Guardar el Stage al inicializar
        finishButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                this.stage = (Stage) newScene.getWindow();
            }
        });
    }

    @FXML
    private void handleFinish() {
        logger.info("Cerrando ventana y pasando a segundo plano");
        setupSystemTray();
        stage.hide();
        runBackgroundService();
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            logger.warn("La bandeja del sistema no está soportada en este equipo");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        try {
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
            trayIcon = new TrayIcon(image, "MediaServer");
            trayIcon.setImageAutoSize(true);

            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Salir");
            exitItem.addActionListener(e -> {
                logger.info("Saliendo de la aplicación");
                tray.remove(trayIcon);
                Platform.exit();
                System.exit(0);
            });
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && !e.isConsumed()) {
                        Platform.runLater(() -> {
                            try {
                                if (serverRegistrationScene == null) {
                                    // Cargar la escena la primera vez
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ServerRegistration.fxml"));
                                    Parent root = loader.load();
                                    ServerRegistrationController controller = loader.getController();
                                    controller.setToken(token);
                                    serverRegistrationScene = new Scene(root, 400, 400);
                                }
                                stage.setScene(serverRegistrationScene);
                                stage.show();
                                stage.toFront();
                                logger.info("Ventana restaurada desde la bandeja");
                            } catch (IOException ex) {
                                logger.error("Error al restaurar ventana", ex);
                            }
                        });
                    }
                }
            });

            tray.add(trayIcon);
            trayIcon.displayMessage("MediaServer", "Minimizado a la bandeja", TrayIcon.MessageType.INFO);
            logger.info("Ícono añadido a la bandeja del sistema");
        } catch (AWTException e) {
            logger.error("Error al configurar la bandeja del sistema", e);
        }
    }

    private void runBackgroundService() {
        new Thread(() -> {
            logger.info("MediaServer ejecutándose en segundo plano...");
            while (true) {
                try {
                    Thread.sleep(60000);
                    logger.info("MediaServer sigue activo...");
                } catch (InterruptedException e) {
                    logger.error("Servicio en segundo plano interrumpido", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
}