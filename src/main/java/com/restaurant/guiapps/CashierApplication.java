package com.restaurant.guiapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.models.Dish;
import com.restaurant.models.Enums.EOrderStatus;
import com.restaurant.models.Enums.EPaymentStatus;
import com.restaurant.models.Enums.EPaymentType;
import com.restaurant.models.Enums.ERole;
import com.restaurant.models.Order;
import com.restaurant.models.Role;
import com.restaurant.models.User;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class CashierApplication extends Application {

    private static final int MAX_QUANTITY_PER_PRODUCT = 20;
    private final Map<Dish, Integer> cart = new HashMap<>();
    private final Label totalPriceLabel = new Label();
    private final Label totalCaloriesLabel = new Label();
    private final VBox orderButtonBox = new VBox();
    private final Tab cartTab = new Tab("CART");
    private String jwtToken;

    @Override
    public void start(Stage primaryStage) {
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
        primaryStage.setTitle("Cashier Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void postLoginProcess(Stage primaryStage, RestTemplate restTemplate) {
        Button payOrderButton = new Button("Opłać zamówienie");
        payOrderButton.setMinSize(310, 150);
        payOrderButton.setOnAction(event -> {
            displayUnpaidOrders(primaryStage, restTemplate);
        });

        Button newOrderButton = new Button("Nowe zamówienie");
        newOrderButton.setMinSize(310, 150);
        newOrderButton.setOnAction(event -> {
            displayNewOrderMenu(primaryStage, restTemplate);
        });

        HBox hbox = new HBox(payOrderButton, newOrderButton);
        hbox.setPadding(new Insets(10));
        hbox.setSpacing(10);

        Scene scene = new Scene(hbox, 650, 820);
        primaryStage.setScene(scene);
    }

    private void displayNewOrderMenu(Stage primaryStage, RestTemplate restTemplate) {
        // Pobierz listę dostępnych dań
        ResponseEntity<List<Dish>> dishResponse = restTemplate.exchange("http://localhost:8080/dishes/available", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        if (dishResponse.getStatusCode() == HttpStatus.OK) {
            List<Dish> dishes = dishResponse.getBody();

            // Grupowanie dań według kategorii
            assert dishes != null;
            Map<String, List<Dish>> dishesByCategory = dishes.stream()
                    .collect(Collectors.groupingBy(dish -> dish.getCategory().name())); // Convert ECategory to String

            TabPane tabPane = new TabPane();

            // Tworzenie zakładki "ALL" z wszystkimi daniami
            GridPane allDishesGridPane = createGridPaneForDishes(dishes);
            Tab allTab = new Tab("ALL");
            allTab.setContent(allDishesGridPane);
            tabPane.getTabs().add(allTab);

            // Tworzenie zakładki dla każdej kategorii
            for (Map.Entry<String, List<Dish>> entry : dishesByCategory.entrySet()) {
                String category = entry.getKey();
                List<Dish> dishesInCategory = entry.getValue();

                // Utwórz zawartość zakładki
                GridPane gridPane = createGridPaneForDishes(dishesInCategory);

                Tab tab = new Tab(category);
                tab.setContent(gridPane);
                tabPane.getTabs().add(tab);
            }

            GridPane cartGridPane = createGridPaneForCart(); // Metoda do implementacji
            cartTab.setContent(cartGridPane);
            tabPane.getTabs().add(cartTab);

            VBox totalsBox = new VBox(totalPriceLabel, totalCaloriesLabel);
            totalsBox.setAlignment(Pos.BOTTOM_LEFT);

            Button placeOrderButton = new Button("Place Order");
            orderButtonBox.getChildren().add(placeOrderButton);

            BorderPane mainLayout = new BorderPane();
            mainLayout.setCenter(tabPane); // TabPane na środku
            mainLayout.setBottom(totalsBox);

            Scene scene = new Scene(mainLayout, 650, 820); // Use mainLayout as the root node
            primaryStage.setScene(scene);
        }
    }

    private GridPane createGridPaneForDishes(List<Dish> dishes) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int row = 0;

        for (Dish dish : dishes) {
            Label dishNameLabel = new Label(dish.getDishName());
            dishNameLabel.setFont(new Font(20));
            Label dishPriceLabel = new Label(dish.getPrice() + " zł");
            dishPriceLabel.setFont(new Font(20));

            Button addButton = new Button("Dodaj");
            addButton.setOnAction(event -> {
                // Add the dish to the cart with a quantity of 1, or increment the quantity if it's already in the cart
                cart.put(dish, cart.getOrDefault(dish, 0) + 1);
                // Update the cart view
                updateCartViewForGridPane();
            });

            Button removeButton = new Button("Usuń");
            removeButton.setOnAction(event -> {
                // Decrease the quantity of the dish in the cart by 1, or remove it if the quantity is 0
                int currentQuantity = cart.getOrDefault(dish, 0);
                if (currentQuantity > 1) {
                    cart.put(dish, currentQuantity - 1);
                } else {
                    cart.remove(dish);
                }
                // Update the cart view
                updateCartViewForGridPane();
            });

            // Utwórz HBox dla nazwy dania, ceny i przycisków "Dodaj" i "Usuń"
            HBox dishInfoBox = new HBox(dishNameLabel, dishPriceLabel, addButton, removeButton);
            dishInfoBox.setSpacing(10);

            VBox dishBox = new VBox(dishInfoBox);
            dishBox.setAlignment(Pos.CENTER);

            gridPane.add(dishBox, 0, row);

            row++;
        }

        return gridPane;
    }

    private GridPane createGridPaneForCart() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int row = 0;

        for (Map.Entry<Dish, Integer> entry : cart.entrySet()) {
            Dish dish = entry.getKey();
            Integer quantity = entry.getValue();

            Label dishLabel = new Label(dish.getDishName() + " - " + String.format("%.2f", dish.getPrice()) + " zł x" + quantity);
            dishLabel.setFont(new Font(20));

            Button changeQuantityButton = new Button("Zmień ilość");
            changeQuantityButton.setOnAction(event -> {
                TextInputDialog dialog = new TextInputDialog(quantity.toString());
                dialog.setTitle("Zmień ilość");
                dialog.setHeaderText("Wprowadź nową ilość dla " + dish.getDishName());
                dialog.setContentText("Ilość:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(s -> {
                    try {
                        int newQuantity = Integer.parseInt(s);
                        if (newQuantity > 0 && newQuantity <= MAX_QUANTITY_PER_PRODUCT) {
                            cart.put(dish, newQuantity);
                        } else if (newQuantity > MAX_QUANTITY_PER_PRODUCT) {
                            // Show an error message if the new quantity is greater than 20
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Błąd");
                            alert.setHeaderText(null);
                            alert.setContentText("Maksymalna ilość dania to " + MAX_QUANTITY_PER_PRODUCT);
                            alert.showAndWait();
                        } else {
                            cart.remove(dish);
                        }
                        updateCartViewForGridPane();
                    } catch (NumberFormatException e) {
                        // Handle invalid input
                    }
                });
            });

            Button removeButton = new Button("Usuń");
            removeButton.setOnAction(event -> {
                cart.remove(dish);
                updateCartViewForGridPane();
            });

            HBox dishBox = new HBox(dishLabel, changeQuantityButton, removeButton);
            dishBox.setSpacing(10);

            gridPane.add(dishBox, 0, row);
            row++;
        }

        return gridPane;
    }

    private void updateCartViewForGridPane() {
        double totalPrice = cart.entrySet().stream().mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue()).sum();
        totalPriceLabel.setText("Cena całkowita: " + String.format("%.2f", totalPrice) + " zł");
        totalPriceLabel.setFont(new Font(20));

        int totalCalories = cart.entrySet().stream().mapToInt(entry -> entry.getKey().getCalories() * entry.getValue()).sum();
        totalCaloriesLabel.setText("Kalorie: " + totalCalories + " kcal");
        totalCaloriesLabel.setFont(new Font(20));

        GridPane cartGridPane = createGridPaneForCart();
        cartTab.setContent(cartGridPane);

        // Check if the cart is empty
        boolean isCartEmpty = cart.isEmpty();

        // Set the disabled property of the "Place Order" button based on whether the cart is empty
        Button placeOrderButton = (Button) orderButtonBox.getChildren().get(0);
        placeOrderButton.setDisable(isCartEmpty);
    }

    private List<Order> getUnpaidOrders(RestTemplate restTemplate) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<Order> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Order>> orderResponse = restTemplate.exchange("http://localhost:8080/orders/kioskOrders", HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        return orderResponse.getBody();
    }

    private void displayUnpaidOrders(Stage primaryStage, RestTemplate restTemplate) {
        List<Order> orders = getUnpaidOrders(restTemplate);

        VBox vbox = new VBox();
        vbox.setSpacing(10);

        Button backButton = new Button("Cofnij");
        backButton.setOnAction(event -> postLoginProcess(primaryStage, restTemplate));
        vbox.getChildren().add(backButton);

        for (Order order : orders) {
            Label orderLabel = new Label("Order Number: " + order.getOrderNumber() + ", Total Cost: " + order.getTotalCost());

            Button payButton = new Button("Zapłać");
            payButton.setOnAction(event -> displayPaymentOptions(primaryStage ,restTemplate, order));

            Button cancelButton = new Button("Anuluj zamówienie");
            cancelButton.setOnAction(event -> cancelOrder(restTemplate, order, primaryStage));

            HBox hbox = new HBox(orderLabel, payButton, cancelButton);
            hbox.setSpacing(10);

            vbox.getChildren().add(hbox);
        }

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setScene(scene);
    }

    private void cancelOrder(RestTemplate restTemplate, Order order, Stage primaryStage) {
        Order updatedOrder = new Order();
        updatedOrder.setOrderStatus(EOrderStatus.CANCELLED);
        updatedOrder.setPaymentStatus(EPaymentStatus.CANCELLED);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        HttpEntity<Order> entity = new HttpEntity<>(updatedOrder, headers);
        ResponseEntity<Order> response = restTemplate.exchange("http://localhost:8080/orders/update/" + order.getOrderId(), HttpMethod.PUT, entity, Order.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Order cancelled successfully");
        } else {
            System.out.println("Failed to cancel order");
        }
        postLoginProcess(primaryStage, restTemplate);
    }

    private void displayPaymentOptions(Stage primaryStage, RestTemplate restTemplate, Order order) {
        Stage paymentStage = new Stage();
        paymentStage.setTitle("Payment Options");

        Button cardPaymentButton = new Button("Płatność kartą");
        cardPaymentButton.setOnAction(event -> {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(Objects.requireNonNull(getClass().getResource("/sounds/payment.mp3")).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();

            order.setPaymentType(EPaymentType.CARD);
            markOrderAsPaid(restTemplate, order);
            paymentStage.close();
            postLoginProcess(primaryStage, restTemplate);
        });

        Button cashPaymentButton = new Button("Płatność gotówką");
        cashPaymentButton.setOnAction(event -> {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(Objects.requireNonNull(getClass().getResource("/sounds/cashregister.mp3")).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();

            order.setPaymentType(EPaymentType.CASH);
            markOrderAsPaid(restTemplate, order);
            paymentStage.close();
            postLoginProcess(primaryStage, restTemplate);
        });

        Button cancelButton = new Button("Anuluj");
        cancelButton.setOnAction(event -> {
            paymentStage.close();
        });

        VBox vbox = new VBox(cardPaymentButton, cashPaymentButton, cancelButton);
        vbox.setSpacing(10);

        Scene scene = new Scene(vbox, 200, 100);
        paymentStage.setScene(scene);
        paymentStage.show();
    }

    private void markOrderAsPaid(RestTemplate restTemplate, Order order) {

        order.setPaymentStatus(EPaymentStatus.PAID);
        order.setOrderStatus(EOrderStatus.PREPARING);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<Order> entity = new HttpEntity<>(order, headers);
        ResponseEntity<Order> response = restTemplate.exchange("http://localhost:8080/orders/update/" + order.getOrderId(), HttpMethod.PUT, entity, Order.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            printReceipt(order);
        } else {
            System.out.println("Failed to mark order " + order.getOrderNumber() + " as paid.");
        }
    }

    private void printReceipt(Order order) {
        System.out.println("====================================");
        System.out.println("Paragon");
        System.out.println("====================================");

        Map<Dish, Long> dishCounts = order.getOrderedDishes().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (Map.Entry<Dish, Long> entry : dishCounts.entrySet()) {
            Dish dish = entry.getKey();
            Long quantity = entry.getValue();
            System.out.println(dish.getDishName() + " x"  + quantity + String.format(" - %.2f", dish.getPrice()*quantity) + " zł");
        }
        System.out.println("------------------------------------");
        System.out.println("Total price: " + order.getTotalCost() + " zł");
        System.out.println("====================================");
    }

    public static void main(String[] args) {
        launch(args);
    }
}