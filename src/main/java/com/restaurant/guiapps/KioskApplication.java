package com.restaurant.guiapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.models.Dish;
import com.restaurant.models.Enums.*;
import com.restaurant.models.Order;
import com.restaurant.models.Role;
import com.restaurant.models.User;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.media.MediaPlayer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class KioskApplication extends Application {

    private static final int MAX_QUANTITY_PER_PRODUCT = 20;
    private final Map<Dish, Integer> cart = new HashMap<>();
    private final Label totalPriceLabel = new Label("Total price: 0.00 zł");
    private final VBox cartVBox = new VBox();
    private final VBox orderButtonBox = new VBox();
    private final TextArea additionalNotesTextArea = new TextArea();
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

        //RestTemplate restTemplate = new RestTemplate();

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
        primaryStage.setTitle("Kiosk Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane createGridPaneForDishes(List<Dish> dishes) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        int row = 0;
        int column = 0;

        for (Dish dish : dishes) {
            Label dishNameLabel = new Label(dish.getDishName());
            dishNameLabel.setFont(new Font(20));
            Label dishPriceLabel = new Label(dish.getPrice() + " zł");
            dishPriceLabel.setFont(new Font(20));

            String imagePath = dish.getImageAddress().replaceFirst("src/main/resources", "");
            Image dishImage = new Image(Objects.requireNonNull(getClass().getResource(imagePath)).toExternalForm());
            ImageView dishImageView = new ImageView(dishImage);
            dishImageView.setFitWidth(300);
            dishImageView.setFitHeight(300);

            dishImageView.setOnMouseClicked(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Dodaj do koszyka");
                alert.setHeaderText("Czy dodać produkt do koszyka?");
                alert.setContentText("Produkt: " + dish.getDishName());

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK){
                    int currentQuantity = cart.getOrDefault(dish, 0);
                    if (currentQuantity + 1 > MAX_QUANTITY_PER_PRODUCT) {
                        Alert quantityAlert = new Alert(Alert.AlertType.WARNING);
                        quantityAlert.setTitle("Limit przekroczony");
                        quantityAlert.setHeaderText(null);
                        quantityAlert.setContentText("Nie możesz dodać więcej niż " + MAX_QUANTITY_PER_PRODUCT + " sztuk tego samego produktu do koszyka.");
                        quantityAlert.showAndWait();
                    } else {
                        cart.put(dish, currentQuantity + 1);
                        updateTotalPrice();
                    }
                }
            });

            VBox dishBox = new VBox(dishNameLabel, dishImageView, dishPriceLabel);
            dishBox.setAlignment(Pos.CENTER);

            gridPane.add(dishBox, column, row);

            column++;
            if (column >= 2) {
                column = 0;
                row++;
            }
        }

        return gridPane;
    }

    private final Label totalCaloriesLabel = new Label("Total calories: 0 kcal");
    private void updateTotalPrice() {
        double totalPrice = cart.entrySet().stream().mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue()).sum();
        totalPriceLabel.setText("Cena całkowita: " + String.format("%.2f", totalPrice) + " zł");
        totalPriceLabel.setFont(new Font(20));

        int totalCalories = cart.entrySet().stream().mapToInt(entry -> entry.getKey().getCalories() * entry.getValue()).sum();
        totalCaloriesLabel.setText("Kalorie: " + totalCalories + " kcal");
        totalCaloriesLabel.setFont(new Font(20));

        updateCartView();

        // Check if the cart is empty
        boolean isCartEmpty = cart.isEmpty();

        // Set the disabled property of the "Place Order" button based on whether the cart is empty
        Button placeOrderButton = (Button) orderButtonBox.getChildren().get(0);
        placeOrderButton.setDisable(isCartEmpty);
    }

    private void updateCartView() {
        cartVBox.getChildren().clear();
        for (Map.Entry<Dish, Integer> entry : cart.entrySet()) {
            Dish dish = entry.getKey();
            Integer quantity = entry.getValue();

            Label dishLabel = new Label(dish.getDishName() + " - " + String.format("%.2f", dish.getPrice()) + " zł x" + quantity);
            Button removeButton = new Button("Usuń");
            removeButton.setOnAction(event -> {
                cart.remove(dish);
                updateTotalPrice();
            });

            Button changeQuantityButton = new Button("Zmień ilość");
            changeQuantityButton.setOnAction(event -> {
                TextInputDialog dialog = new TextInputDialog(quantity.toString());
                dialog.setTitle("Zmień ilość");
                dialog.setHeaderText("Wprowadź nową ilość produktu");
                dialog.setContentText("Ilość:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(s -> {
                    try {
                        int newQuantity = Integer.parseInt(s);
                        if (newQuantity > 0 && newQuantity <= MAX_QUANTITY_PER_PRODUCT) {
                            cart.put(dish, newQuantity);
                        } else {
                            Alert quantityAlert = new Alert(Alert.AlertType.WARNING);
                            quantityAlert.setTitle("Limit przekroczony");
                            quantityAlert.setHeaderText(null);
                            quantityAlert.setContentText("Nie możesz ustawić więcej niż " + MAX_QUANTITY_PER_PRODUCT + " sztuk tego samego produktu.");
                            quantityAlert.showAndWait();
                        }
                        updateTotalPrice();
                    } catch (NumberFormatException e) {
                        // ignore invalid input
                    }
                });
            });

            HBox dishBox = new HBox(dishLabel, removeButton, changeQuantityButton);
            dishBox.setSpacing(10);
            cartVBox.getChildren().add(dishBox);
        }
        additionalNotesTextArea.setPromptText("Additional notes");

        // Add the TextArea to the cartVBox
        cartVBox.getChildren().add(additionalNotesTextArea);

        cartVBox.getChildren().add(orderButtonBox);
    }

    private void postLoginProcess(Stage primaryStage, RestTemplate restTemplate) {
        Button cartButton = new Button("Koszyk");
        VBox newVbox = new VBox();
        newVbox.setPadding(new Insets(10));
        newVbox.setSpacing(8);

        Button placeOrderButton = new Button("Złóż zamówienie");
        placeOrderButton.setOnAction(placeOrderEvent -> {
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.setTitle("Potwierdzenie zamówienia");
            confirmationAlert.setHeaderText("Czy na pewno chcesz złożyć zamówienie?");

            Optional<ButtonType> result = confirmationAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {

                // Użytkownik potwierdził zamówienie, więc zmieniamy całe obecne okno
                newVbox.getChildren().clear();

                Button payWithCardButton = new Button("Zapłać kartą");
                Button payWithCashButton = new Button("Zapłać gotówką w kasie");
                Button cancelOrderButton = new Button("Anuluj zamówienie");

                newVbox.getChildren().addAll(payWithCardButton, payWithCashButton, cancelOrderButton);

// Tworzymy nową instancję VBox i kopiujemy do niej zawartość newVbox
                VBox newSceneVbox = new VBox();
                newSceneVbox.getChildren().addAll(newVbox.getChildren());

// Ustawiamy odstęp między elementami na nowym VBox
                newSceneVbox.setSpacing(10);

                Scene scene = new Scene(newSceneVbox, 650, 820);
                primaryStage.setScene(scene);
                primaryStage.show();

                cancelOrderButton.setOnAction(cancelEvent -> {
                    // Użytkownik anulował zamówienie, więc wracamy do menu głównego z wyzerowanym koszykiem
                    cart.clear();
                    additionalNotesTextArea.clear();
                    updateTotalPrice();
                    newVbox.getChildren().clear();
                    postLoginProcess(primaryStage, restTemplate);
                });

                payWithCardButton.setOnAction(event -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Płatność kartą");
                    alert.setHeaderText("Postępuj zgodnie z instrukcjami na terminalu");
                    alert.show();

                    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), ae -> alert.close()));
                    timeline.play();

                    PauseTransition delay = new PauseTransition(Duration.seconds(3));
//                    delay.setOnFinished( eventBox -> alert.close() );
//                    delay.play();

                    // Odtwarzanie dźwięku
                    javafx.scene.media.Media sound = new javafx.scene.media.Media(Objects.requireNonNull(getClass().getResource("/sounds/payment.mp3")).toExternalForm());
                    MediaPlayer mediaPlayer = new MediaPlayer(sound);
                    mediaPlayer.play();


                    delay.setOnFinished( eventDelay -> Platform.runLater(() -> {
                        Alert transactionAlert = new Alert(Alert.AlertType.INFORMATION);
                        transactionAlert.setTitle("Płatność kartą");
                        transactionAlert.setHeaderText("Transakcja przebiegła pomyślnie");
                        transactionAlert.setContentText("Odbierz paragon i numer zamówienia");
                        transactionAlert.show();

                        Order order = new Order();
                        List<Dish> orderedDishes = new ArrayList<>();
                        for (Map.Entry<Dish, Integer> entry : cart.entrySet()) {
                            Dish dish = entry.getKey();
                            Integer quantity = entry.getValue();
                            for (int i = 0; i < quantity; i++) {
                                orderedDishes.add(dish);
                            }
                        }
                        order.setOrderedDishes(orderedDishes);
                        order.setTotalCost(order.calculateTotalCost());
                        order.setOrderStatus(EOrderStatus.PREPARING);
                        order.setPaymentType(EPaymentType.CARD);
                        order.setPaymentStatus(EPaymentStatus.PAID);
                        order.setDeliveryMethod(EDeliveryMethod.PICKUP);
                        order.setAdditionalNotes(additionalNotesTextArea.getText());
                        order.setDeliveryInfo(" ");
                        System.out.println(order.getOrderNumber());

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(jwtToken);
                        HttpEntity<Order> entity = new HttpEntity<>(order, headers);
                        ResponseEntity<Order> response = restTemplate.exchange("http://localhost:8080/orders", HttpMethod.POST, entity, Order.class);

                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            printReceipt();
                            Order savedOrder = response.getBody();
                            assert savedOrder != null;
                            printOrderNumber(savedOrder, true);
                        } else {
                            System.out.println("Nie udało się zapisać zamówienia w bazie danych.");
                        }

                        Timeline transactionTimeline = new Timeline(new KeyFrame(Duration.seconds(3), ae -> transactionAlert.close()));
                        transactionTimeline.play();

                        PauseTransition delayAfterAlert = new PauseTransition(Duration.seconds(1));
                        delayAfterAlert.setOnFinished(eventAfterAlert -> {
                            cart.clear();
                            additionalNotesTextArea.clear();
                            updateTotalPrice();
                            newVbox.getChildren().clear();
                            postLoginProcess(primaryStage, restTemplate);
                        });
                        delayAfterAlert.play();
                    }));
                    delay.play();
                });

                payWithCashButton.setOnAction(event -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Płatność gotówką");
                    alert.setHeaderText("Odbierz numer zamówienia i podejdź do kasy");
                    alert.show();

                    Timeline delay = new Timeline(new KeyFrame(Duration.seconds(3), ae -> Platform.runLater(() -> {
                        alert.close();

                        Order order = new Order();
                        List<Dish> orderedDishes = new ArrayList<>();
                        for (Map.Entry<Dish, Integer> entry : cart.entrySet()) {
                            Dish dish = entry.getKey();
                            Integer quantity = entry.getValue();
                            for (int i = 0; i < quantity; i++) {
                                orderedDishes.add(dish);
                            }
                        }
                        order.setOrderedDishes(orderedDishes);
                        order.setTotalCost(order.calculateTotalCost());
                        order.setOrderStatus(EOrderStatus.PLACED);
                        order.setPaymentType(EPaymentType.CASH);
                        order.setPaymentStatus(EPaymentStatus.UNPAID);
                        order.setDeliveryMethod(EDeliveryMethod.PICKUP);
                        order.setAdditionalNotes(additionalNotesTextArea.getText());
                        order.setDeliveryInfo(" ");

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setBearerAuth(jwtToken);
                        HttpEntity<Order> entity = new HttpEntity<>(order, headers);
                        ResponseEntity<Order> response = restTemplate.exchange("http://localhost:8080/orders", HttpMethod.POST, entity, Order.class);

                        if (response.getStatusCode() == HttpStatus.CREATED) {
                            Order savedOrder = response.getBody();
                            assert savedOrder != null;
                            printOrderNumber(savedOrder, false);
                        } else {
                            System.out.println("Nie udało się zapisać zamówienia w bazie danych.");
                        }

                        cart.clear();
                        additionalNotesTextArea.clear();
                        updateTotalPrice();
                        newVbox.getChildren().clear();
                        postLoginProcess(primaryStage, restTemplate);
                    })));
                    delay.play();
                });
            }
        });

        if (orderButtonBox.getChildren().isEmpty()) {
            orderButtonBox.getChildren().add(placeOrderButton);
        }

        Tab cartTab = new Tab("Koszyk");

        ResponseEntity<List<Dish>> dishResponse = restTemplate.exchange("http://localhost:8080/dishes/available", HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });
        if (dishResponse.getStatusCode() == HttpStatus.OK) {
            List<Dish> dishes = dishResponse.getBody();
            assert dishes != null;

            TabPane tabPane = new TabPane();

            Tab allDishesTab = new Tab("Wszystkie");
            GridPane allDishesGridPane = createGridPaneForDishes(dishes);
            ScrollPane allDishesScrollPane = new ScrollPane(allDishesGridPane);
            allDishesScrollPane.setFitToWidth(true);
            allDishesTab.setContent(allDishesScrollPane);
            tabPane.getTabs().add(allDishesTab);

            for (ECategory category : ECategory.values()) {
                Tab tab = new Tab(category.name());
                GridPane gridPane = createGridPaneForDishes(dishes.stream()
                        .filter(dish -> dish.getCategory() == category)
                        .collect(Collectors.toList()));
                ScrollPane scrollPane = new ScrollPane(gridPane);
                scrollPane.setFitToWidth(true);
                tab.setContent(scrollPane);
                tabPane.getTabs().add(tab);
            }

            cartTab.setContent(cartVBox);
            tabPane.getTabs().add(cartTab);

            cartButton.setOnAction(cartEvent -> tabPane.getSelectionModel().select(cartTab));

            newVbox.getChildren().add(tabPane);
            newVbox.getChildren().add(totalPriceLabel);
            newVbox.getChildren().add(totalCaloriesLabel);
            newVbox.getChildren().add(cartButton);

            Scene scene = new Scene(newVbox, 640, 820);
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    private void printReceipt() {
        System.out.println("====================================");
        System.out.println("Paragon");
        System.out.println("====================================");
        for (Map.Entry<Dish, Integer> entry : cart.entrySet()) {
            Dish dish = entry.getKey();
            Integer quantity = entry.getValue();
            System.out.println(dish.getDishName() + " x"  + quantity + String.format(" - %.2f", dish.getPrice()*quantity) + " zł");
        }
        System.out.println("------------------------------------");
        System.out.println(totalPriceLabel.getText());
        System.out.println(totalCaloriesLabel.getText());
        System.out.println("====================================");
    }

    private void printOrderNumber(Order order, boolean paid) {
        System.out.println("====================================");
        System.out.println("Numer zamówienia");
        System.out.println("====================================");
        System.out.println("Zamówienie nr: " + order.getOrderNumber());
        System.out.println("====================================");
        if (paid) {
            System.out.println("Odbierz zamówienie w punkcie odbioru");
        } else {
            System.out.println("Zapłać za zamówienie w kasie");
        }
        System.out.println("====================================\n\n\n\n\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}