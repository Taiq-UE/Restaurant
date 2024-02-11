package com.restaurant.guiapps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.models.Dish;
import com.restaurant.models.Enums.ECategory;
import com.restaurant.models.Enums.ERole;
import com.restaurant.models.Role;
import com.restaurant.models.User;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminPanelApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(AdminPanelApplication.class);
    private String jwtToken;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) {
        login(stage);
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
                if (eRoles.contains(ERole.ROLE_ADMIN)) {
                    vbox.getChildren().clear();
                    postLoginProcess(primaryStage, restTemplate);
                }
                else {
                    System.out.println("You are not an admin");
                }
            }
        });

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Admin Panel Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void postLoginProcess(Stage primaryStage, RestTemplate restTemplate) {
        Button employeesManagementButton = new Button("EMPLOYEES MANAGMENT");
        employeesManagementButton.setOnAction(event -> employeesManagement(primaryStage, restTemplate));

        Button dishesUpdateButton = new Button("DISHES UPDATE");
        dishesUpdateButton.setOnAction(event -> dishManagement(primaryStage, restTemplate));

        Button remoteStartButton = new Button("REMOTE START");
        remoteStartButton.setOnAction(event -> remoteStartManager(primaryStage, restTemplate));

        Button logoutButton = new Button("LOG OUT");
        logoutButton.setOnAction(event -> {
            jwtToken = null;
            login(primaryStage);
        });

        VBox vbox = new VBox(employeesManagementButton, dishesUpdateButton, remoteStartButton, logoutButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Admin Panel Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void employeesManagement(Stage primaryStage, RestTemplate restTemplate) {
        Button addEmployeeButton = new Button("ADD EMPLOYEE");
        addEmployeeButton.setOnAction(event -> addEmployee(primaryStage, restTemplate));

        Button removeEmployeeButton = new Button("REMOVE EMPLOYEE");
        removeEmployeeButton.setOnAction(event -> removeEmployee(primaryStage, restTemplate));

        Button displayEmployeesButton = new Button("DISPLAY EMPLOYEES");
        displayEmployeesButton.setOnAction(event -> displayEmployees(primaryStage, restTemplate));

        Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(event -> postLoginProcess(primaryStage, restTemplate));

        VBox vbox = new VBox(addEmployeeButton, removeEmployeeButton, displayEmployeesButton, cancelButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Employees Management");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void addEmployee(Stage primaryStage, RestTemplate restTemplate) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        CheckBox isAdminCheckBox = new CheckBox("Is Admin");

        Button addEmployeeButton = new Button("ADD EMPLOYEE");

        Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(event -> employeesManagement(primaryStage, restTemplate));

        VBox vbox = new VBox(usernameField, passwordField, isAdminCheckBox, addEmployeeButton, cancelButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        addEmployeeButton.setOnAction(event -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtToken);
            String requestJson = "{ \"username\": \"" + usernameField.getText() + "\", \"password\": \"" + passwordField.getText() + "\", \"role\": [\"" + (isAdminCheckBox.isSelected() ? "admin" : "employee") + "\"] }";
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/users/register", HttpMethod.POST, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Employee added");
            }
            postLoginProcess(primaryStage, restTemplate);
        });

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Add Employee");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void displayEmployees(Stage primaryStage, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/users/all", HttpMethod.GET, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<User> users;
            try {
                users = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            VBox vbox = new VBox();
            vbox.setPadding(new Insets(10));
            vbox.setSpacing(8);

            for (User user : users) {
                String roles = user.getRoles().stream()
                        .map(role -> role.getName().name().substring(5))
                        .collect(Collectors.joining(", "));
                vbox.getChildren().add(new Label("Username: " + user.getUsername() + ", Roles: " + roles));
            }

            Button backButton = new Button("BACK");
            backButton.setOnAction(event -> employeesManagement(primaryStage, restTemplate));

            vbox.getChildren().add(backButton);

            Scene scene = new Scene(vbox, 650, 820);
            primaryStage.setTitle("Display Employees");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    private void removeEmployee(Stage primaryStage, RestTemplate restTemplate) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        Button removeEmployeeButton = new Button("REMOVE EMPLOYEE");

        Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(event -> employeesManagement(primaryStage, restTemplate));

        VBox vbox = new VBox(usernameField, removeEmployeeButton, cancelButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        removeEmployeeButton.setOnAction(event -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/users/delete/" + usernameField.getText(), HttpMethod.DELETE, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Employee removed");
            }
            postLoginProcess(primaryStage, restTemplate);
        });

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Remove Employee");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void dishManagement(Stage primaryStage, RestTemplate restTemplate) {
        Button addDishButton = new Button("ADD DISH");
        addDishButton.setOnAction(event -> addDish(primaryStage, restTemplate));

        Button removeDishButton = new Button("REMOVE DISH");
        removeDishButton.setOnAction(event -> removeDish(primaryStage, restTemplate));

        Button updateAvailabilityButton = new Button("UPDATE AVAILABILITY");
        updateAvailabilityButton.setOnAction(event -> updateAvailability(primaryStage, restTemplate));

        Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(event -> postLoginProcess(primaryStage, restTemplate));

        VBox vbox = new VBox(addDishButton, removeDishButton, updateAvailabilityButton, cancelButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Dish Management");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void addDish(Stage primaryStage, RestTemplate restTemplate) {
        TextField dishNameField = new TextField();
        dishNameField.setPromptText("Dish Name");

        TextField priceField = new TextField();
        priceField.setPromptText("Price");

        TextField caloriesField = new TextField();
        caloriesField.setPromptText("Calories");

        ComboBox<ECategory> categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().setAll(ECategory.values());
        categoryComboBox.setPromptText("Category");

        TextField imageAddressField = new TextField();
        imageAddressField.setPromptText("Image Address");

        CheckBox isAvailableCheckBox = new CheckBox("Is Available");

        Button addDishButton = new Button("ADD DISH");

        Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(event -> dishManagement(primaryStage, restTemplate));

        VBox vbox = new VBox(dishNameField, priceField, caloriesField, categoryComboBox, imageAddressField, isAvailableCheckBox, addDishButton, cancelButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        addDishButton.setOnAction(event -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(jwtToken);
            String requestJson = "{ \"dishName\": \"" + dishNameField.getText() + "\", \"price\": " + priceField.getText() + ", \"calories\": " + caloriesField.getText() + ", \"category\": \"" + categoryComboBox.getValue() + "\", \"imageAddress\": \"" + imageAddressField.getText() + "\", \"isAvailable\": " + isAvailableCheckBox.isSelected() + " }";
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/dishes", HttpMethod.POST, entity, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                System.out.println("Dish added");
            }
            dishManagement(primaryStage, restTemplate);
        });

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Add Dish");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void removeDish(Stage primaryStage, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/dishes/all", HttpMethod.GET, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Dish> dishes;
            try {
                dishes = objectMapper.readValue(response.getBody(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            VBox vbox = new VBox();
            vbox.setPadding(new Insets(10));
            vbox.setSpacing(8);

            for (Dish dish : dishes) {
                Button deleteButton = new Button("Delete");
                deleteButton.setOnAction(event -> {
                    deleteDish(dish, restTemplate);
                    removeDish(primaryStage, restTemplate);
                });
                vbox.getChildren().add(new HBox(new Label(dish.getDishName()), deleteButton));
            }

            Button cancelButton = new Button("CANCEL");
            cancelButton.setOnAction(event -> dishManagement(primaryStage, restTemplate));
            vbox.getChildren().add(cancelButton);

            Scene scene = new Scene(vbox, 650, 820);
            primaryStage.setTitle("Remove Dish");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    private void deleteDish(Dish dish, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange("http://localhost:8080/dishes/" + dish.getDishId(), HttpMethod.DELETE, entity, Void.class);
    }

    private void updateAvailability(Stage primaryStage, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/dishes/all", HttpMethod.GET, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Dish> dishes;
            try {
                dishes = objectMapper.readValue(response.getBody(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            VBox vbox = new VBox();
            vbox.setPadding(new Insets(10));
            vbox.setSpacing(8);

            for (Dish dish : dishes) {
                Button toggleButton = new Button(dish.getIsAvailable() ? "Make Unavailable" : "Make Available");
                toggleButton.setOnAction(event -> {
                    toggleAvailability(dish, restTemplate);
                    updateAvailability(primaryStage, restTemplate);
                });
                vbox.getChildren().add(new HBox(new Label(dish.getDishName()), toggleButton));
            }

            Button cancelButton = new Button("CANCEL");
            cancelButton.setOnAction(event -> dishManagement(primaryStage, restTemplate));
            vbox.getChildren().add(cancelButton);

            Scene scene = new Scene(vbox, 650, 820);
            primaryStage.setTitle("Update Availability");
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    private void toggleAvailability(Dish dish, RestTemplate restTemplate) {
        dish.setIsAvailable(!dish.getIsAvailable());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);
        HttpEntity<Dish> entity = new HttpEntity<>(dish, headers);
        restTemplate.exchange("http://localhost:8080/dishes/update/" + dish.getDishId(), HttpMethod.PUT, entity, Dish.class);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private OrdersStatusApplication ordersStatusApp;
    private OrdersStatusChangeApplication ordersStatusChangeApp;
    private KitchenApplication kitchenApp;
    private CashierApplication cashierApp;
    private KioskApplication kioskApp;
    private void remoteStartManager(Stage primaryStage, RestTemplate restTemplate) {

        Button ordersStatusButton = new Button("ORDERS STATUS");
        ordersStatusButton.setOnAction(event -> {
            ordersStatusApp = new OrdersStatusApplication(); // Zapisz referencję do aplikacji
            Platform.runLater(() -> {
                try {
                    Stage newStage = new Stage();
                    ordersStatusApp.remoteLogin(newStage, new RestTemplate(), jwtToken);
                } catch (Exception e) {
                    logger.error("Error during remote login", e);
                }
            });
        });

        Button ordersStatusStopButton = new Button("STOP ORDERS STATUS");
        ordersStatusStopButton.setOnAction(event -> {
            if (ordersStatusApp != null) {
                ordersStatusApp.closeWindow();
            }
        });

        Button ordersStatusChangeButton = new Button("ORDERS STATUS CHANGE");
        ordersStatusChangeButton.setOnAction(event -> {
            ordersStatusChangeApp = new OrdersStatusChangeApplication(); // Zapisz referencję do aplikacji
            Platform.runLater(() -> {
                try {
                    Stage newStage = new Stage();
                    ordersStatusChangeApp.remoteLogin(newStage, new RestTemplate(), jwtToken);
                } catch (Exception e) {
                    logger.error("Error during remote login", e);
                }
            });
        });

        Button ordersStatusChangeStopButton = new Button("STOP ORDERS STATUS CHANGE"); // Nowy przycisk do zatrzymania
        ordersStatusChangeStopButton.setOnAction(event -> {
            if (ordersStatusChangeApp != null) {
                ordersStatusChangeApp.closeWindow(); // Wywołaj metodę closeWindow na aplikacji
            }
        });

        Button kitchenButton = new Button("KITCHEN");
        kitchenButton.setOnAction(event -> {
            kitchenApp = new KitchenApplication(); // Zapisz referencję do aplikacji
            Platform.runLater(() -> {
                try {
                    Stage newStage = new Stage();
                    kitchenApp.remoteLogin(newStage, new RestTemplate(), jwtToken);
                } catch (Exception e) {
                    logger.error("Error during remote login", e);
                }
            });
        });

        Button kitchenStopButton = new Button("STOP KITCHEN");
        kitchenStopButton.setOnAction(event -> {
            if (kitchenApp != null) {
                kitchenApp.closeWindow();
            }
        });

        Button cashRegisterButton = new Button("CASH REGISTER");
        cashRegisterButton.setOnAction(event -> {
            cashierApp = new CashierApplication(); // Zapisz referencję do aplikacji
            Platform.runLater(() -> {
                try {
                    Stage newStage = new Stage();
                    cashierApp.remoteLogin(newStage, new RestTemplate(), jwtToken);
                } catch (Exception e) {
                    logger.error("Error during remote login", e);
                }
            });
        });

        Button cashRegisterStopButton = new Button("STOP CASH REGISTER"); // Nowy przycisk do zatrzymania
        cashRegisterStopButton.setOnAction(event -> {
            if (cashierApp != null) {
                cashierApp.closeWindow(); // Wywołaj metodę closeWindow na aplikacji
            }
        });

        Button kioskButton = new Button("KIOSK");
        kioskButton.setOnAction(event -> {
            kioskApp = new KioskApplication(); // Zapisz referencję do aplikacji
            Platform.runLater(() -> {
                try {
                    Stage newStage = new Stage();
                    kioskApp.remoteLogin(newStage, new RestTemplate(), jwtToken);
                } catch (Exception e) {
                    logger.error("Error during remote login", e);
                }
            });
        });

        Button kioskStopButton = new Button("STOP KIOSK");
        kioskStopButton.setOnAction(event -> {
            if (kioskApp != null) {
                kioskApp.closeWindow();
            }
        });

        Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(event -> postLoginProcess(primaryStage, restTemplate));

        VBox vbox = new VBox(ordersStatusButton, ordersStatusStopButton, ordersStatusChangeButton, ordersStatusChangeStopButton, kitchenButton, kitchenStopButton, cashRegisterButton, cashRegisterStopButton, kioskButton,kioskStopButton, cancelButton);
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Scene scene = new Scene(vbox, 650, 820);
        primaryStage.setTitle("Remote Start Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

}


