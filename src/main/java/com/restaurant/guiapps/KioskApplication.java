package com.restaurant.guiapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.models.Dish;
import com.restaurant.models.ECategory;
import com.restaurant.models.ERole;
import com.restaurant.models.Role;
import com.restaurant.models.User;
import javafx.application.Application;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class KioskApplication extends Application {

    private static final int MAX_QUANTITY_PER_PRODUCT = 20;
    private Map<Dish, Integer> cart = new HashMap<>();
    private Label totalPriceLabel = new Label("Total price: 0.00 zł");
    private VBox cartVBox = new VBox();

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
                User user = null;
                try {
                    user = new ObjectMapper().readValue(response.getBody(), User.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                Set<Role> roles = user.getRoles();
                List<ERole> eRoles = roles.stream()
                        .map(Role::getName)
                        .collect(Collectors.toList());
                if (eRoles.contains(ERole.ROLE_EMPLOYEE) || eRoles.contains(ERole.ROLE_ADMIN)) {
                    vbox.getChildren().clear();

                    Button cartButton = new Button("Koszyk");
                    VBox newVbox = new VBox();
                    newVbox.setPadding(new Insets(10));
                    newVbox.setSpacing(8);

                    ResponseEntity<List<Dish>> dishResponse = restTemplate.exchange("http://localhost:8080/dishes/available", HttpMethod.GET, null, new ParameterizedTypeReference<List<Dish>>() {});
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

                        Tab cartTab = new Tab("Koszyk");
                        //ListView<String> cartListView = new ListView<>();
                        cartTab.setContent(cartVBox);
                        tabPane.getTabs().add(cartTab);

                        cartButton.setOnAction(cartEvent -> {
                            tabPane.getSelectionModel().select(cartTab);
                        });

                        newVbox.getChildren().add(tabPane);
                        newVbox.getChildren().add(totalPriceLabel);
                        newVbox.getChildren().add(cartButton);

                        Scene scene = new Scene(newVbox, 620, 820);
                        primaryStage.setScene(scene);
                        primaryStage.show();
                    }
                }
            }
        });

        Scene scene = new Scene(vbox, 620, 820);
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
            Label dishPriceLabel = new Label(String.valueOf(dish.getPrice()) + " zł");
            dishPriceLabel.setFont(new Font(20));

            String imagePath = dish.getImageAddress().replaceFirst("src/main/resources", "");
            Image dishImage = new Image(getClass().getResource(imagePath).toExternalForm());
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
                        if (totalPriceLabel != null) {
                            updateTotalPrice();
                        }
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

    private void updateTotalPrice() {
        double totalPrice = cart.entrySet().stream().mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue()).sum();
        totalPriceLabel.setText("Total price: " + String.format("%.2f", totalPrice) + " zł");
        totalPriceLabel.setFont(new Font(20));
        updateCartView();
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}