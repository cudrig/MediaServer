package com.miapp.mediaserver;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class ServerRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(ServerRegistrationController.class);
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML private TextField serverNameField;
    @FXML private Button saveButton;
    @FXML private Label statusLabel;
    @FXML private ListView<ServerResponse> serverList;

    private String token;

    public void setToken(String token) {
        this.token = token;
        loadServers();
    }

    @FXML
    private void initialize() {
        serverNameField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleSave(); });
    }

    @FXML
    private void handleSave() {
        String serverName = serverNameField.getText();
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            int port = 8080;

            String json = String.format("{\"serverName\":\"%s\",\"ipAddress\":\"%s\",\"port\":%d}", 
                    serverName, ipAddress, port);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(BASE_URL + "/register-server")
                    .header("Authorization", token)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    ServerResponse serverResponse = mapper.readValue(responseBody, ServerResponse.class);
                    logger.info("MediaServer registrado: {}", serverName);
                    statusLabel.setText("Servidor registrado con éxito");
                    loadServers();
                    showServerConfigured(serverResponse.getStreamKey(), serverResponse.getIpAddress() + ":" + serverResponse.getPort());
                } else {
                    logger.warn("Fallo al registrar servidor: {}", serverName);
                    statusLabel.setText("Error al registrar servidor: " + response.body().string());
                }
            }
        } catch (IOException e) {
            logger.error("Error al registrar MediaServer", e);
            statusLabel.setText("Error de conexión");
        }
    }

    private void loadServers() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/servers")
                .header("Authorization", token)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                List<ServerResponse> servers = mapper.readValue(responseBody, new TypeReference<List<ServerResponse>>(){});
                serverList.getItems().clear();
                serverList.getItems().addAll(servers);
                serverList.setCellFactory(param -> new ListCell<ServerResponse>() {
                    @Override
                    protected void updateItem(ServerResponse item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox(10);
                            Circle statusCircle = new Circle(5);
                            statusCircle.setFill("ONLINE".equals(item.getStatus()) ? Color.GREEN : Color.RED);
                            Label nameLabel = new Label(item.getName());
                            hbox.getChildren().addAll(statusCircle, nameLabel);
                            setGraphic(hbox);
                        }
                    }
                });
            } else {
                logger.warn("Fallo al cargar servidores: {}", response.code());
                statusLabel.setText("Error al cargar servidores");
            }
        } catch (IOException e) {
            logger.error("Error al cargar servidores", e);
            statusLabel.setText("Error al cargar servidores");
        }
    }

    private void showServerConfigured(String streamKey, String streamingUrl) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ServerConfigured.fxml"));
        Parent root = loader.load();
        ServerConfiguredController controller = loader.getController();
        controller.setToken(token);
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.setScene(new Scene(root, 400, 200));
        stage.setTitle("MediaServer - Configuración Completada");
    }

    public static class ServerResponse {
        private Long id;
        private String name;
        private String ipAddress;
        private int port;
        private String streamKey;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getStreamKey() { return streamKey; }
        public void setStreamKey(String streamKey) { this.streamKey = streamKey; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}