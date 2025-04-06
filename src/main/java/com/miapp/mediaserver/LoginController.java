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

public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private String token;

    @FXML
    private void initialize() {
        emailField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleLogin(); });
        passwordField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleLogin(); });
    }

    @FXML
    private void handleLogin() {
        String identifier = emailField.getText();
        String password = passwordField.getText();

        String jsonEmail = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", identifier, password);
        RequestBody bodyEmail = RequestBody.create(jsonEmail, MediaType.parse("application/json"));
        Request requestEmail = new Request.Builder()
                .url(BASE_URL + "/login")
                .post(bodyEmail)
                .build();

        try (Response response = client.newCall(requestEmail).execute()) {
            if (response.isSuccessful()) {
                processSuccessfulLogin(response);
            } else {
                String jsonUsername = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", identifier, password);
                RequestBody bodyUsername = RequestBody.create(jsonUsername, MediaType.parse("application/json"));
                Request requestUsername = new Request.Builder()
                        .url(BASE_URL + "/login")
                        .post(bodyUsername)
                        .build();

                try (Response usernameResponse = client.newCall(requestUsername).execute()) {
                    if (usernameResponse.isSuccessful()) {
                        processSuccessfulLogin(usernameResponse);
                    } else {
                        logger.warn("Login fallido para: {}", identifier);
                        statusLabel.setText("Credenciales incorrectas");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error al iniciar sesión", e);
            statusLabel.setText("Error de conexión");
        }
    }

    private void processSuccessfulLogin(Response response) throws IOException {
        String responseBody = response.body().string();
        UserResponse user = mapper.readValue(responseBody, UserResponse.class);
        token = user.getToken();
        logger.info("Login exitoso para: {}", emailField.getText());
        statusLabel.setText("Login exitoso");
        showServerRegistration();
    }

    @FXML
    private void handleBack() throws IOException {
        logger.info("Volviendo a la ventana de bienvenida");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Welcome.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(new Scene(root, 400, 300));
        stage.setTitle("MediaServer");
    }

    private void showServerRegistration() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ServerRegistration.fxml"));
        Parent root = loader.load();
        ServerRegistrationController controller = loader.getController();
        controller.setToken(token);
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(new Scene(root, 400, 400));
        stage.setTitle("MediaServer - Registro de Servidor");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserResponse {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}