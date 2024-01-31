package com.restaurant.guiapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.models.Enums.ERole;
import com.restaurant.models.Order;
import com.restaurant.models.Role;
import com.restaurant.models.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

public class OrdersStatusApplication extends Application {

    private String jwtToken;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        login(primaryStage);
    }

    private void login(Stage primaryStage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button();
        loginButton.setText("Log in");

        VBox vbox = new VBox(usernameField, passwordField, loginButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        loginButton.setOnAction(event -> {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestJson = "{ \"username\": \"" + usernameField.getText() + "\", \"password\": \"" + passwordField.getText() + "\" }";
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/users/login", HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode;
                try {
                    jsonNode = objectMapper.readTree(responseBody);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                jwtToken = jsonNode.get("accessToken").asText();
                User user;
                try {
                    user = new ObjectMapper().readValue(response.getBody(), User.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                Set<Role> roles = user.getRoles();
                List<ERole> eRoles = roles.stream()
                        .map(Role::getName)
                        .toList();
                if (eRoles.contains(ERole.ROLE_EMPLOYEE) || eRoles.contains(ERole.ROLE_ADMIN)) {
                    vbox.getChildren().clear();
                    postLoginProcess(primaryStage, restTemplate);
                }
            }
        });

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Orders status Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void postLoginProcess(Stage primaryStage, RestTemplate restTemplate) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            List<Order> preparingOrders = getOrders(restTemplate, "preparing");
            List<Order> readyOrders = getOrders(restTemplate, "ready");
            orderDisplay(primaryStage, preparingOrders, readyOrders);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);
    }

    private List<Order> getOrders(RestTemplate restTemplate, String orderStatus) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Order>> response = restTemplate.exchange(
                "http://localhost:8080/orders/" + orderStatus,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {});
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get preparing orders");
        }
    }

    private void orderDisplay(Stage primaryStage, List<Order> preparingOrders, List<Order> readyOrders) {
        VBox leftVbox = new VBox();
        leftVbox.setPadding(new Insets(10));
        leftVbox.setSpacing(8);

        Label preparingLabel = new Label("PREPARING");
        preparingLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #2c2b2b;");
        leftVbox.getChildren().add(preparingLabel);

        for (Order order : preparingOrders) {
            Label orderLabel = new Label(String.valueOf(order.getOrderId()));
            orderLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #2c2b2b;");
            leftVbox.getChildren().add(orderLabel);
        }

        VBox rightVbox = new VBox();
        rightVbox.setPadding(new Insets(10));
        rightVbox.setSpacing(8);

        Label readyLabel = new Label("READY");
        readyLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #27d927;");
        rightVbox.getChildren().add(readyLabel);

        for (Order order : readyOrders) {
            Label orderLabel = new Label(String.valueOf(order.getOrderId()));
            orderLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #27d927;");
            rightVbox.getChildren().add(orderLabel);
        }

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftVbox, rightVbox);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(splitPane);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 650, 820);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}