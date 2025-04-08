package com.miapp.mediaserver;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainMenuController {
    private static final Logger logger = LoggerFactory.getLogger(MainMenuController.class);
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private static final String TOKEN_FILE = "token.txt";
    private final OkHttpClient client = new OkHttpClient();

    private String token;
    private TrayIcon trayIcon;

    @FXML private Button serversButton;
    @FXML private Button exitButton;
    @FXML private Button logoutButton;

    public void setToken(String token) {
        this.token = token;
    }

    @FXML
    private void handleServers() {
        logger.info("Abriendo pestaña de configuración de servidores");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ServerRegistration.fxml"));
            if (loader.getLocation() == null) {
                logger.error("No se pudo encontrar ServerRegistration.fxml en /fxml/ServerRegistration.fxml");
                return;
            }
            Parent root = loader.load();
            ServerRegistrationController controller = loader.getController();
            if (controller == null) {
                logger.error("El controlador de ServerRegistration.fxml es null");
                return;
            }
            controller.setToken(token);
            Stage stage = (Stage) serversButton.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 400));
            stage.setTitle("MediaServer - Registro de Servidor");
            logger.info("ServerRegistration.fxml mostrado exitosamente");
        } catch (IOException e) {
            logger.error("Error al abrir la pestaña de servidores", e);
        }
    }

    @FXML
    private void handleExit() {
        logger.info("Minimizando aplicación a segundo plano");
        Stage stage = (Stage) exitButton.getScene().getWindow();
        minimizeToTray(stage);
    }

    @FXML
    private void handleLogout() {
        logger.info("Cerrando sesión y volviendo a la pantalla de inicio");
        if (token == null) {
            logger.warn("Token nulo al intentar cerrar sesión");
        } else {
            logger.info("Enviando solicitud de logout con token: {}", token);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/logout")
                    .header("Authorization", "Bearer " + token)
                    .post(RequestBody.create("", null))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                logger.info("Código de respuesta del logout: {}", response.code());
                if (response.isSuccessful()) {
                    logger.info("Token invalidado exitosamente en el backend");
                } else {
                    logger.warn("Fallo al invalidar token, código: {}", response.code());
                    if (response.body() != null) {
                        logger.warn("Mensaje del servidor: {}", response.body().string());
                    }
                }
            } catch (IOException e) {
                logger.error("Error al cerrar sesión en el backend", e);
            }
        }

        // Eliminar el token localmente solo al cerrar sesión
        File tokenFile = new File(TOKEN_FILE);
        if (tokenFile.exists() && !tokenFile.delete()) {
            logger.warn("No se pudo eliminar el archivo de token: {}", TOKEN_FILE);
        } else {
            logger.info("Token local eliminado: {}", TOKEN_FILE);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Welcome.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root, 400, 300);
            stage.setScene(scene);
            stage.setTitle("MediaServer");
            logger.info("Welcome.fxml cargado exitosamente tras cerrar sesión");
        } catch (IOException e) {
            logger.error("Error al cargar Welcome.fxml", e);
            throw new RuntimeException("No se pudo cargar Welcome.fxml", e);
        }
    }

    private void minimizeToTray(Stage stage) {
        if (!SystemTray.isSupported()) {
            logger.warn("La bandeja del sistema no está soportada");
            return;
        }
        SystemTray tray = SystemTray.getSystemTray();
        try {
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
            if (image == null) {
                logger.error("No se pudo cargar el ícono desde /icon.png");
                return;
            }
            trayIcon = new TrayIcon(image, "MediaServer");
            trayIcon.setImageAutoSize(true);

            PopupMenu popup = new PopupMenu();
            MenuItem restoreItem = new MenuItem("Restaurar");
            restoreItem.addActionListener(e -> {
                logger.info("Restaurando la aplicación desde la bandeja");
                Platform.runLater(() -> {
                    stage.show();
                    tray.remove(trayIcon);
                });
            });
            MenuItem exitItem = new MenuItem("Salir");
            exitItem.addActionListener(e -> {
                logger.info("Saliendo de la aplicación desde la bandeja, manteniendo token");
                tray.remove(trayIcon);
                Platform.exit();
                System.exit(0); // El token permanece en token.txt
            });
            popup.add(restoreItem);
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);
            tray.add(trayIcon);
            trayIcon.displayMessage("MediaServer", "Minimizado a la bandeja", TrayIcon.MessageType.INFO);
            logger.info("Icono añadido a la bandeja del sistema");
            stage.hide();
        } catch (AWTException e) {
            logger.error("Error al configurar la bandeja del sistema", e);
        }
    }
}