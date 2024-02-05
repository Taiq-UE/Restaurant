package com.restaurant.guiapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.models.Dish;
import com.restaurant.models.Enums.*;
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
    private final VBox cartVBox = new VBox();
    private final Tab cartTab = new Tab("CART");
    private final TabPane tabPane = new TabPane();
    private final TextArea additionalNotesTextArea = new TextArea();
    private Stage primaryStage;
    private String jwtToken;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
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

    void remoteLogin(Stage primaryStage, RestTemplate restTemplate, String jwt){
        jwtToken = jwt;
        postLoginProcess(primaryStage, restTemplate);
        this.primaryStage = primaryStage;
    }
    private void postLoginProcess(Stage primaryStage, RestTemplate restTemplate) {

        Button payOrderButton = new Button("Opłać zamówienie");
        payOrderButton.setMinSize(310, 150);
        payOrderButton.setOnAction(event -> displayUnpaidOrders(primaryStage, restTemplate));

        Button newOrderButton = new Button("Nowe zamówienie");
        newOrderButton.setMinSize(310, 150);
        newOrderButton.setOnAction(event -> displayNewOrderMenu(primaryStage, restTemplate));

        HBox hbox = new HBox(payOrderButton, newOrderButton);
        hbox.setPadding(new Insets(10));
        hbox.setSpacing(10);

        Scene scene = new Scene(hbox, 650, 820);
        primaryStage.setTitle("Cashier Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void displayNewOrderMenu(Stage primaryStage, RestTemplate restTemplate) {

        Button backButton = new Button("Cofnij");
        backButton.setOnAction(event -> {
            cart.clear();
            updateCartViewForGridPane();

            int tabIndex = 0;
            if (tabPane.getTabs().size() > tabIndex) {
                tabPane.getSelectionModel().select(tabIndex);
            }
            postLoginProcess(primaryStage, restTemplate);
        });

        VBox vbox = new VBox(backButton);
        vbox.setSpacing(10);
        ResponseEntity<List<Dish>> dishResponse = restTemplate.exchange("http://localhost:8080/dishes/available", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        if (dishResponse.getStatusCode() == HttpStatus.OK) {
            List<Dish> dishes = dishResponse.getBody();

            assert dishes != null;
            Map<String, List<Dish>> dishesByCategory = dishes.stream()
                    .collect(Collectors.groupingBy(dish -> dish.getCategory().name())); // Convert ECategory to String

            TabPane tabPane = new TabPane();

            GridPane allDishesGridPane = createGridPaneForDishes(dishes);
            Tab allTab = new Tab("ALL");
            allTab.setContent(allDishesGridPane);
            tabPane.getTabs().add(allTab);

            for (Map.Entry<String, List<Dish>> entry : dishesByCategory.entrySet()) {
                String category = entry.getKey();
                List<Dish> dishesInCategory = entry.getValue();

                GridPane gridPane = createGridPaneForDishes(dishesInCategory);

                Tab tab = new Tab(category);
                tab.setContent(gridPane);
                tabPane.getTabs().add(tab);
            }

            GridPane cartGridPane = createGridPaneForCart();
            cartTab.setContent(cartGridPane);
            tabPane.getTabs().add(cartTab);

            VBox totalsBox = new VBox(totalPriceLabel, totalCaloriesLabel, orderButtonBox);
            totalsBox.setAlignment(Pos.BOTTOM_LEFT);

            if (!cartVBox.getChildren().contains(additionalNotesTextArea)) {
                additionalNotesTextArea.setPromptText("Additional notes");
                cartVBox.getChildren().add(additionalNotesTextArea);
            }

            additionalNotesTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.length() > 255) {
                    additionalNotesTextArea.setText(oldValue);
                }
            });

            totalsBox.getChildren().add(cartVBox);

            orderButtonBox.getChildren().clear();

            Button placeOrderButton = new Button("Place Order");
            placeOrderButton.setOnAction(event -> {
                Order order = new Order();
                order.setOrderedDishes(cart.keySet().stream().toList());
                order.setTotalCost(order.calculateTotalCost());
                order.setOrderStatus(EOrderStatus.PLACED);
                order.setPaymentStatus(EPaymentStatus.UNPAID);
                order.setDeliveryMethod(EDeliveryMethod.PICKUP);
                order.setAdditionalNotes(additionalNotesTextArea.getText());
                order.setDeliveryInfo(" ");
                displayPaymentOptions(primaryStage, restTemplate, order, false);

                tabPane.getSelectionModel().select(0);
                vbox.getChildren().addAll(tabPane, totalsBox);
            });
            orderButtonBox.getChildren().add(placeOrderButton);

            BorderPane mainLayout = new BorderPane();
            mainLayout.setTop(vbox);
            mainLayout.setCenter(tabPane);
            mainLayout.setBottom(totalsBox);

            Scene scene = new Scene(mainLayout, 650, 820);
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
                int currentQuantity = cart.getOrDefault(dish, 0);
                if (currentQuantity < MAX_QUANTITY_PER_PRODUCT) {
                    cart.put(dish, currentQuantity + 1);
                    updateCartViewForGridPane();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Błąd");
                    alert.setHeaderText(null);
                    alert.setContentText("Maksymalna ilość dania to " + MAX_QUANTITY_PER_PRODUCT);
                    alert.showAndWait();
                }
            });

            Button removeButton = new Button("Usuń");
            removeButton.setOnAction(event -> {
                int currentQuantity = cart.getOrDefault(dish, 0);
                if (currentQuantity > 1) {
                    cart.put(dish, currentQuantity - 1);
                } else {
                    cart.remove(dish);
                }
                updateCartViewForGridPane();
            });

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
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Błąd");
                            alert.setHeaderText(null);
                            alert.setContentText("Maksymalna ilość dania to " + MAX_QUANTITY_PER_PRODUCT);
                            alert.showAndWait();
                        } else {
                            cart.remove(dish);
                        }
                        updateCartViewForGridPane();
                    } catch (NumberFormatException ignored) {
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

        boolean isCartEmpty = cart.isEmpty();

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
            payButton.setOnAction(event -> displayPaymentOptions(primaryStage ,restTemplate, order, true));

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

    private void displayPaymentOptions(Stage primaryStage, RestTemplate restTemplate, Order order, boolean existingOrder) {
        Stage paymentStage = new Stage();
        paymentStage.setTitle("Payment Options");

        VBox vbox = new VBox();
        vbox.getChildren().clear();

        Button cardPaymentButton = new Button("Płatność kartą");
        cardPaymentButton.setOnAction(event -> {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(Objects.requireNonNull(getClass().getResource("/sounds/payment.mp3")).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();

            order.setPaymentType(EPaymentType.CARD);
            if (existingOrder) {
                markOrderAsPaid(restTemplate, order);
            }
            else {
                placeOrder(restTemplate, order);
            }
            paymentStage.close();
            postLoginProcess(primaryStage, restTemplate);
        });

        Button cashPaymentButton = new Button("Płatność gotówką");
        cashPaymentButton.setOnAction(event -> {
            javafx.scene.media.Media sound = new javafx.scene.media.Media(Objects.requireNonNull(getClass().getResource("/sounds/cashregister.mp3")).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();

            order.setPaymentType(EPaymentType.CASH);
            if (existingOrder) {
                markOrderAsPaid(restTemplate, order);
            }
            else {
                placeOrder(restTemplate, order);
            }
            paymentStage.close();
            postLoginProcess(primaryStage, restTemplate);
        });

        Button cancelButton = new Button("Anuluj");
        cancelButton.setOnAction(event -> paymentStage.close());

        vbox = new VBox(cardPaymentButton, cashPaymentButton, cancelButton);
        vbox.setSpacing(10);

        Scene scene = new Scene(vbox, 200, 100);
        paymentStage.setScene(scene);
        paymentStage.show();
    }

    private void placeOrder(RestTemplate restTemplate, Order order) {
        order.setPaymentStatus(EPaymentStatus.PAID);
        order.setOrderStatus(EOrderStatus.PREPARING);

        // Create a new list to hold the dishes
        List<Dish> orderedDishes = new ArrayList<>();

        // For each entry in the cart
        for (Map.Entry<Dish, Integer> entry : cart.entrySet()) {
            // Get the dish and the quantity
            Dish dish = entry.getKey();
            int quantity = entry.getValue();

            // Add the dish to the list as many times as the quantity
            for (int i = 0; i < quantity; i++) {
                orderedDishes.add(dish);
            }
        }

        // Set the ordered dishes in the order
        order.setOrderedDishes(orderedDishes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<Order> entity = new HttpEntity<>(order, headers);
        ResponseEntity<Order> response = restTemplate.exchange("http://localhost:8080/orders", HttpMethod.POST, entity, Order.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            Order savedOrder = response.getBody();
            System.out.println("Order placed successfully");
            printReceipt(order);
            assert savedOrder != null;
            printOrderNumber(savedOrder);
            additionalNotesTextArea.clear();
            cart.clear();
        } else {
            System.out.println("Failed to place order");
        }
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

    private void printOrderNumber(Order order) {
        System.out.println("====================================");
        System.out.println("Numer zamówienia");
        System.out.println("====================================");
        System.out.println("Zamówienie nr: " + order.getOrderNumber());
        System.out.println("====================================");
        System.out.println("Odbierz zamówienie w punkcie odbioru");
        System.out.println("====================================\n\n\n\n\n");
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void closeWindow() {
        primaryStage.close();
    }
}