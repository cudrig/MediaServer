package com.miapp.mediaserver;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;

public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        usernameField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleRegister(); });
        emailField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleRegister(); });
        passwordField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleRegister(); });
        confirmPasswordField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleRegister(); });
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validaciones locales
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Rellene todos los campos");
            return;
        }
        if (!email.matches(".*@.*\\..+")) {
            statusLabel.setText("El email debe contener @ y un dominio válido (ej. .es, .com, .ru)");
            return;
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Las contraseñas no coinciden");
            return;
        }
        if (password.length() < 8 || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%&]).*$")) {
            statusLabel.setText("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial (!@#$%&)");
            return;
        }

        String registerJson = String.format("{\"user\":{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"},\"confirmPassword\":\"%s\"}",
                username, email, password, confirmPassword);
        RequestBody registerBody = RequestBody.create(registerJson, MediaType.parse("application/json"));
        Request registerRequest = new Request.Builder()
                .url(BASE_URL + "/register")
                .post(registerBody)
                .build();

        try (Response response = client.newCall(registerRequest).execute()) {
            if (response.isSuccessful()) {
                logger.info("Usuario registrado: {}", email);
                statusLabel.setText("Registro exitoso, iniciando sesión...");
                performLogin(email, password);
            } else {
                String errorMsg = response.body() != null ? response.body().string() : "Error desconocido";
                logger.warn("Registro fallido para: {}, mensaje: {}", email, errorMsg);
                statusLabel.setText("Error al registrar: " + errorMsg);
            }
        } catch (IOException e) {
            logger.error("Error al registrar", e);
            statusLabel.setText("Error de conexión");
        }
    }

    private void performLogin(String email, String password) throws IOException {
        String loginJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        RequestBody loginBody = RequestBody.create(loginJson, MediaType.parse("application/json"));
        Request loginRequest = new Request.Builder()
                .url(BASE_URL + "/login")
                .post(loginBody)
                .build();
        try (Response response = client.newCall(loginRequest).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                UserResponse user = mapper.readValue(responseBody, UserResponse.class);
                String token = user.getToken();
                logger.info("Login automático exitoso para: {}", email);
                showMainMenu(token);
            } else {
                logger.warn("Login automático fallido para: {}", email);
                statusLabel.setText("Registro OK, pero error al iniciar sesión");
            }
        }
    }

    @FXML
    private void handleBack() throws IOException {
        logger.info("Volviendo a la ventana de bienvenida");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Welcome.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("MediaServer");
    }

    private void showMainMenu(String token) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainMenu.fxml"));
        Parent root = loader.load();
        MainMenuController controller = loader.getController();
        controller.setToken(token);
        Stage stage = (Stage) registerButton.getScene().getWindow();
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("MediaServer - Menú Principal");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserResponse {
        private String token;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}