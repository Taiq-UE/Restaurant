package com.restaurant.guiapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.models.Dish;
import com.restaurant.models.Enums.EOrderStatus;
import com.restaurant.models.Enums.ERole;
import com.restaurant.models.Order;
import com.restaurant.models.Role;
import com.restaurant.models.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OrdersStatusChangeApplication extends Application {

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
        primaryStage.setTitle("Order status change Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    void remoteLogin(Stage primaryStage, RestTemplate restTemplate, String jwt){
        jwtToken = jwt;
        postLoginProcess(primaryStage, restTemplate);
    }

    private void postLoginProcess(Stage primaryStage, RestTemplate restTemplate) {

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            List<Order> preparingOrders = getPreparingOrders(restTemplate);
            orderDisplay(primaryStage, preparingOrders, restTemplate);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        primaryStage.setOnCloseRequest(event -> timeline.stop());

        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Orders status change Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private List<Order> getPreparingOrders(RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Order>> response = restTemplate.exchange(
                "http://localhost:8080/orders/ready",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {});
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get preparing orders");
        }
    }

    private void orderDisplay(Stage primaryStage, List<Order> preparingOrders, RestTemplate restTemplate) {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        for (Order order : preparingOrders) {
            BorderPane orderPane = new BorderPane();
            orderPane.setPadding(new Insets(10));
            orderPane.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-border-radius: 5;");

            VBox detailsBox = new VBox();
            Label orderIdLabel = new Label("Order ID: " + order.getOrderId());
            orderIdLabel.setStyle("-fx-font-size: 20px;");
            VBox dishesBox = new VBox();

            Map<String, Long> dishCounts = order.getOrderedDishes().stream()
                    .collect(Collectors.groupingBy(Dish::getDishName, Collectors.counting()));

            for (Map.Entry<String, Long> entry : dishCounts.entrySet()) {
                Label dishLabel = new Label(entry.getKey() + " - Quantity: " + entry.getValue());
                dishLabel.setStyle("-fx-font-size: 20px;");
                dishesBox.getChildren().add(dishLabel);
            }

            if (!order.getAdditionalNotes().isBlank()) {
                Label notesLabel = new Label("\nAdditional notes: \n" + order.getAdditionalNotes());
                notesLabel.setStyle("-fx-font-size: 20px;");
                detailsBox.getChildren().addAll(orderIdLabel, dishesBox, notesLabel);
            }
            else {
                detailsBox.getChildren().addAll(orderIdLabel, dishesBox);
            }

            Button doneButton = new Button("Delivered");
            doneButton.setPrefWidth(100);
            doneButton.setPrefHeight(50);
            doneButton.setOnAction(event -> {
                setOrderStatusDelivered(restTemplate, order);
                preparingOrders.remove(order);
                orderDisplay(primaryStage, preparingOrders, restTemplate);
            });

            orderPane.setCenter(detailsBox);
            orderPane.setRight(doneButton);
            vbox.getChildren().add(orderPane);
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(vbox);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 650, 820);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setOrderStatusDelivered(RestTemplate restTemplate, Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        order.setOrderStatus(EOrderStatus.DELIVERED);
        HttpEntity<Order> entity = new HttpEntity<>(order, headers);
        ResponseEntity<Order> response = restTemplate.exchange("http://localhost:8080/orders/update/" + order.getOrderId(), HttpMethod.PUT, entity, Order.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to set order status to READY");
        }

    }

}