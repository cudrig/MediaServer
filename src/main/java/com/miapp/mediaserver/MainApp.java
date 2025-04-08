package com.miapp.mediaserver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private static final String TOKEN_FILE = "token.txt";
    private final OkHttpClient client = new OkHttpClient();
    private TrayIcon trayIcon;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Platform.setImplicitExit(false);
        logger.info("Iniciando MediaServer...");

        if (checkSavedToken(primaryStage)) {
            logger.info("Sesión mantenida, mostrando MainMenu.fxml");
            return;
        }

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Welcome.fxml"));
        primaryStage.setTitle("MediaServer");
        primaryStage.setScene(new Scene(root, 400, 300));

        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            minimizeToTray(primaryStage);
        });

        primaryStage.show();
    }

    private boolean checkSavedToken(Stage stage) {
        try {
            if (Files.exists(Paths.get(TOKEN_FILE))) {
                String token = new String(Files.readAllBytes(Paths.get(TOKEN_FILE))).trim();
                logger.info("Token leído desde {}: {}", TOKEN_FILE, token);
                if (!token.isEmpty() && validateToken(token)) {
                    logger.info("Token válido, cargando MainMenu.fxml");
                    loadMainMenu(token, stage);
                    return true;
                } else {
                    logger.warn("Token inválido, pero no se eliminará para depuración");
                }
            } else {
                logger.info("No se encontró el archivo de token: {}", TOKEN_FILE);
            }
            logger.info("No hay token válido, mostrando pantalla de bienvenida");
            return false;
        } catch (IOException e) {
            logger.error("Error al leer el token guardado", e);
            return false;
        }
    }

    private boolean validateToken(String token) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/servers")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            logger.info("Validación del token, código de respuesta: {}", response.code());
            if (!response.isSuccessful() && response.body() != null) {
                logger.warn("Cuerpo de la respuesta: {}", response.body().string());
            }
            return response.isSuccessful();
        } catch (IOException e) {
            logger.warn("Token inválido o error de conexión", e);
            return false;
        }
    }

    private void loadMainMenu(String token, Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainMenu.fxml"));
        Parent root = loader.load();
        MainMenuController controller = loader.getController();
        controller.setToken(token);
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("MediaServer - Menú Principal");
        stage.show();
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
                System.exit(0);
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

    public static void main(String[] args) {
        launch(args);
    }
}