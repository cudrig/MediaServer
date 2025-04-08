package com.miapp.mediaserver;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WelcomeController {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeController.class);

    @FXML private Button loginButton;
    @FXML private Button registerButton;

    @FXML
    private void handleLogin() throws IOException {
        logger.info("Redirigiendo a la ventana de inicio de sesión");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("MediaServer - Inicio de Sesión");
    }

    @FXML
    private void handleRegister() throws IOException {
        logger.info("Redirigiendo a la ventana de registro");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) registerButton.getScene().getWindow();
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("MediaServer - Registro");
    }
}
