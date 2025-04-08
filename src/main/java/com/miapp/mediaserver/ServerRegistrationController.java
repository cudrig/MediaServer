package com.miapp.mediaserver;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ServerRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(ServerRegistrationController.class);
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private Timeline statusCheckTimeline;

    @FXML private TextField serverNameField;
    @FXML private Button saveButton;
    @FXML private Button acceptButton;
    @FXML private Label statusLabel;
    @FXML private ListView<ServerResponse> serverList;
    @FXML private ProgressIndicator progressIndicator;

    private String token;
    private ServerResponse serverToEdit = null;

    public void setToken(String token) {
        this.token = token;
        loadServers(); // Carga los servidores y actualiza el estado inmediatamente
        startStatusCheck(); // Inicia la verificación periódica
    }

    @FXML
    private void initialize() {
        serverNameField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleSave(); });
        setupServerList();
        acceptButton.setOnAction(event -> handleAccept());
        saveButton.setOnAction(event -> handleSave());
        progressIndicator.setVisible(false);
        setupValidation();
    }

    private void setupValidation() {
        serverNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.trim().isEmpty()) {
                statusLabel.setText("El nombre no puede estar vacío");
                serverNameField.setStyle("-fx-border-color: red;");
            } else if (!newValue.matches("[a-zA-Z0-9-_]+")) {
                statusLabel.setText("Solo letras, números, guiones y guiones bajos");
                serverNameField.setStyle("-fx-border-color: red;");
            } else {
                statusLabel.setText("");
                serverNameField.setStyle("-fx-border-color: none;");
            }
        });
    }

    private void startStatusCheck() {
        statusCheckTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> updateServerStatus()));
        statusCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        statusCheckTimeline.play();
    }

    private void updateServerStatus() {
        for (ServerResponse server : serverList.getItems()) {
            boolean isOnline = checkServerStatus(server.getIpAddress(), server.getPort());
            server.setStatus(isOnline ? "ONLINE" : "OFFLINE");
        }
        serverList.refresh();
    }

    private boolean checkServerStatus(String ipAddress, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @FXML
    private void handleSave() {
        String serverName = serverNameField.getText().trim();
        if (serverName.isEmpty() || !serverName.matches("[a-zA-Z0-9-_]+")) {
            statusLabel.setText("Nombre inválido");
            return;
        }
        statusLabel.setText("Guardando servidor...");
        progressIndicator.setVisible(true);
        if (serverToEdit == null) {
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
                        statusLabel.setText("Servidor '" + serverName + "' registrado con éxito");
                        serverNameField.clear();
                        loadServers();
                    } else {
                        String errorMsg = response.body() != null ? response.body().string() : "Error desconocido";
                        logger.warn("Fallo al registrar servidor: {}, mensaje: {}", serverName, errorMsg);
                        statusLabel.setText("Error al registrar servidor: " + errorMsg);
                    }
                }
            } catch (IOException e) {
                logger.error("Error al registrar MediaServer", e);
                statusLabel.setText("Error de conexión");
            } finally {
                progressIndicator.setVisible(false);
            }
        } else {
            try {
                String json = String.format("{\"serverName\":\"%s\",\"ipAddress\":\"%s\",\"port\":%d}", 
                        serverName, serverToEdit.getIpAddress(), serverToEdit.getPort());
                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(BASE_URL + "/servers/" + serverToEdit.getId())
                        .header("Authorization", token)
                        .put(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        logger.info("Servidor editado: {}", serverName);
                        statusLabel.setText("Servidor '" + serverName + "' actualizado con éxito");
                        serverNameField.clear();
                        serverToEdit = null;
                        saveButton.setText("Guardar");
                        loadServers();
                    } else {
                        String errorMsg = response.body() != null ? response.body().string() : "Error desconocido";
                        logger.warn("Fallo al editar servidor: {}, mensaje: {}", serverName, errorMsg);
                        statusLabel.setText("Error al editar servidor: " + errorMsg);
                    }
                }
            } catch (IOException e) {
                logger.error("Error al editar MediaServer", e);
                statusLabel.setText("Error de conexión");
            } finally {
                progressIndicator.setVisible(false);
            }
        }
    }

    @FXML
    private void handleAccept() {
        logger.info("Volviendo a la pantalla principal (MainMenu.fxml)");
        statusCheckTimeline.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainMenu.fxml"));
            Parent root = loader.load();
            MainMenuController controller = loader.getController();
            controller.setToken(token);
            Stage stage = (Stage) acceptButton.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 300));
            stage.setTitle("MediaServer");
        } catch (IOException e) {
            logger.error("Error al cargar MainMenu.fxml", e);
            statusLabel.setText("Error al volver a la pantalla principal");
        }
    }

    private void setupServerList() {
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
                    Button editButton = new Button("Editar");
                    editButton.setOnAction(event -> editServer(item));
                    Button deleteButton = new Button("Borrar");
                    deleteButton.setOnAction(event -> deleteServer(item));
                    hbox.getChildren().addAll(statusCircle, nameLabel, editButton, deleteButton);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void editServer(ServerResponse server) {
        serverToEdit = server;
        serverNameField.setText(server.getName());
        saveButton.setText("Actualizar");
        statusLabel.setText("Editando servidor '" + server.getName() + "'");
    }

    private void loadServers() {
        statusLabel.setText("Cargando servidores...");
        progressIndicator.setVisible(true);
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
                logger.info("Lista de servidores cargada: {} servidores", servers.size());
                statusLabel.setText("");
                updateServerStatus(); // Comprobación inmediata del estado
            } else {
                String errorMsg = response.body() != null ? response.body().string() : "Error desconocido";
                logger.warn("Fallo al cargar servidores: {}, mensaje: {}", response.code(), errorMsg);
                statusLabel.setText("Error al cargar servidores");
            }
        } catch (IOException e) {
            logger.error("Error al cargar servidores", e);
            statusLabel.setText("Error al cargar servidores");
        } finally {
            progressIndicator.setVisible(false);
        }
    }

    private void deleteServer(ServerResponse server) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Seguro que quieres borrar '" + server.getName() + "'?");
        alert.setContentText("Esta acción no se puede deshacer.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                logger.info("Intentando borrar servidor: {}, ID: {}", server.getName(), server.getId());
                statusLabel.setText("Borrando servidor...");
                progressIndicator.setVisible(true);
                Request request = new Request.Builder()
                        .url(BASE_URL + "/servers/" + server.getId())
                        .header("Authorization", token)
                        .delete()
                        .build();

                try (Response responseServer = client.newCall(request).execute()) {
                    logger.info("Código de respuesta del servidor: {}", responseServer.code());
                    if (responseServer.isSuccessful()) {
                        logger.info("Servidor borrado exitosamente: {}", server.getName());
                        statusLabel.setText("Servidor '" + server.getName() + "' borrado con éxito");
                        loadServers();
                    } else {
                        String errorMsg = responseServer.body() != null ? responseServer.body().string() : "Error desconocido";
                        logger.warn("Fallo al borrar servidor: {}, código: {}, mensaje: {}", server.getName(), responseServer.code(), errorMsg);
                        statusLabel.setText("Error al borrar servidor (código " + responseServer.code() + "): " + errorMsg);
                    }
                } catch (IOException e) {
                    logger.error("Error al borrar servidor: {}", server.getName(), e);
                    statusLabel.setText("Error de conexión al borrar servidor");
                } finally {
                    progressIndicator.setVisible(false);
                }
            }
        });
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